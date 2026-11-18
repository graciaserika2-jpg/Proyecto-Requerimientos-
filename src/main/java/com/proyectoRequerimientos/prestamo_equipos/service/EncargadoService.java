
package com.proyectoRequerimientos.prestamo_equipos.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyectoRequerimientos.prestamo_equipos.model.Encargado;
import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;

@Service
public class EncargadoService {

    private final EncargadoRepository encargadoRepository;
    private final CuentaPersonaRepository cuentaPersonaRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenCuentaService tokenCuentaService;
    private final CorreoService correoService;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String CARACTERES =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public EncargadoService(
            EncargadoRepository encargadoRepository,
            CuentaPersonaRepository cuentaPersonaRepository,
            PasswordEncoder passwordEncoder,
            TokenCuentaService tokenCuentaService,
            CorreoService correoService) {

        this.encargadoRepository = encargadoRepository;
        this.cuentaPersonaRepository = cuentaPersonaRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenCuentaService = tokenCuentaService;
        this.correoService = correoService;
    }

    // =====================================================
    // LISTAR ENCARGADOS Y ADMINISTRADORES
    // =====================================================

    public List<Encargado> listarEncargados() {

        return encargadoRepository.findAll()
                .stream()
                .filter(Encargado::isActivo)
                .filter(encargado ->
                        "ENCARGADO".equalsIgnoreCase(encargado.getRol())
                        || "ADMIN".equalsIgnoreCase(encargado.getRol()))
                .toList();
    }

    // =====================================================
    // CREAR ENCARGADO
    // =====================================================

    @Transactional
    public Encargado crearEncargado(
            String usuario,
            String nombre,
            String correo) {

        return crearCuentaAdministrativa(
                usuario, nombre, correo, "ENCARGADO");
    }

    // =====================================================
    // CREAR PRIMER ADMINISTRADOR
    // =====================================================

    @Transactional
    public synchronized Encargado crearPrimerAdministrador(
            String usuario,
            String nombre,
            String correo) {

        if (encargadoRepository.existsByRolIgnoreCase("ADMIN")) {
            throw new IllegalArgumentException(
                    "La configuración inicial ya fue realizada. "
                    + "Ya existe un administrador.");
        }

        return crearCuentaAdministrativa(
                usuario, nombre, correo, "ADMIN");
    }

    // =====================================================
    // CREAR ADMINISTRADOR DESDE OTRO ADMINISTRADOR
    // =====================================================

    @Transactional
    public Encargado crearAdministrador(
            String usuario,
            String nombre,
            String correo,
            String usuarioSolicitante) {

        validarAdministradorSolicitante(usuarioSolicitante);

        return crearCuentaAdministrativa(
                usuario, nombre, correo, "ADMIN");
    }

    // =====================================================
    // VALIDAR QUIÉN SOLICITA CREAR UN ADMINISTRADOR
    // =====================================================

    private void validarAdministradorSolicitante(String usuarioSolicitante) {

        if (usuarioSolicitante == null || usuarioSolicitante.isBlank()) {
            throw new IllegalArgumentException(
                    "No se pudo identificar al administrador que realiza la operación.");
        }

        Encargado solicitante = encargadoRepository
                .findByUsuarioIgnoreCase(usuarioSolicitante.trim())
                .filter(Encargado::isActivo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta que realiza la operación no existe o está inactiva."));

        if (!"ADMIN".equalsIgnoreCase(solicitante.getRol())) {
            throw new IllegalArgumentException(
                    "No tienes permisos para registrar administradores.");
        }
    }

    // =====================================================
    // MÉTODO GENERAL PARA CREAR CUENTAS ADMINISTRATIVAS
    // =====================================================

    private Encargado crearCuentaAdministrativa(
            String usuario,
            String nombre,
            String correo,
            String rol) {

        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de usuario es obligatorio.");
        }

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre completo es obligatorio.");
        }

        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo electrónico es obligatorio.");
        }

        String usuarioLimpio = usuario.trim();
        String nombreLimpio = nombre.trim();
        String correoLimpio = correo.trim().toLowerCase(Locale.ROOT);

        if (usuarioLimpio.length() > 80) {
            throw new IllegalArgumentException(
                    "El nombre de usuario no puede superar los 80 caracteres.");
        }

        if (nombreLimpio.length() > 120) {
            throw new IllegalArgumentException(
                    "El nombre completo no puede superar los 120 caracteres.");
        }

        if (correoLimpio.length() > 254) {
            throw new IllegalArgumentException(
                    "El correo electrónico es demasiado largo.");
        }

        // Validar formato del correo electrónico.
        if (!correoLimpio.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {

            throw new IllegalArgumentException(
                    "Escribe un correo electrónico válido.");
        }

        if (encargadoRepository
                .findByUsuarioIgnoreCase(usuarioLimpio).isPresent()) {

            throw new IllegalArgumentException(
                    "Ese nombre de usuario ya está registrado.");
        }

        if (encargadoRepository.existsByCorreoIgnoreCase(correoLimpio)) {
            throw new IllegalArgumentException(
                    "Ese correo ya está registrado en otra cuenta administrativa.");
        }

        if (cuentaPersonaRepository.existsByCorreoIgnoreCase(correoLimpio)) {
            throw new IllegalArgumentException(
                    "Ese correo ya está registrado en una cuenta de alumno.");
        }

        Encargado encargado = new Encargado();

        encargado.setUsuario(usuarioLimpio);
        encargado.setNombre(nombreLimpio);
        encargado.setPasswordHash(
                passwordEncoder.encode(generarPasswordAleatoria()));
        encargado.setRol(rol);
        encargado.setActivo(true);
        encargado.setDebeCambiarPassword(true);
        encargado.setCorreo(correoLimpio);
        encargado.setCorreoVerificado(false);

        Encargado encargadoGuardado = encargadoRepository.save(encargado);

        String token = tokenCuentaService.crearTokenActivacion(encargadoGuardado);

        correoService.enviarActivacion(
                correoLimpio,
                nombreLimpio,
                token);

        return encargadoGuardado;
    }

    // =====================================================
    // CAMBIAR CONTRASEÑA
    // =====================================================

    @Transactional
    public void cambiarPassword(
            String usuario,
            String passwordActual,
            String passwordNueva,
            String confirmarPassword) {

        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException(
                    "No se pudo identificar la cuenta.");
        }

        if (passwordActual == null || passwordActual.isBlank()) {
            throw new IllegalArgumentException(
                    "Escribe tu contraseña actual.");
        }

        if (passwordNueva == null || passwordNueva.length() < 12) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe tener al menos 12 caracteres.");
        }

        if (!passwordNueva.equals(confirmarPassword)) {
            throw new IllegalArgumentException(
                    "Las nuevas contraseñas no coinciden.");
        }

        Encargado encargado = encargadoRepository
                .findByUsuarioIgnoreCase(usuario.trim())
                .filter(Encargado::isActivo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no existe o está inactiva."));

        if (!passwordEncoder.matches(
                passwordActual, encargado.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "La contraseña actual es incorrecta.");
        }

        if (passwordEncoder.matches(
                passwordNueva, encargado.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "La nueva contraseña debe ser diferente de la actual.");
        }

        encargado.setPasswordHash(
                passwordEncoder.encode(passwordNueva));

        encargado.setDebeCambiarPassword(false);

        encargadoRepository.save(encargado);
    }

    // =====================================================
    // GENERAR CONTRASEÑA ALEATORIA INTERNA
    // =====================================================

    private String generarPasswordAleatoria() {

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 32; i++) {

            int posicion = RANDOM.nextInt(CARACTERES.length());

            password.append(CARACTERES.charAt(posicion));
        }

        return password.toString();
    }
}

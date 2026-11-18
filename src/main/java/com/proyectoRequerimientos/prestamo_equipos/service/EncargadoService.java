
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
    // VALIDAR ADMINISTRADOR SOLICITANTE
    // =====================================================

    private Encargado validarAdministradorSolicitante(
            String usuarioSolicitante) {

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
                    "No tienes permisos para realizar esta operación.");
        }

        return solicitante;
    }

    // =====================================================
    // EDITAR CUENTA ADMINISTRATIVA
    // =====================================================

    @Transactional
    public Encargado editarCuenta(
            Long id,
            String usuario,
            String nombre,
            String correo,
            String usuarioSolicitante) {

        Encargado solicitante =
                validarAdministradorSolicitante(usuarioSolicitante);

        Encargado encargado = encargadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta que deseas editar no existe."));

        validarCuentaAdministrativa(encargado);

        if (encargado.getId().equals(solicitante.getId())) {
            throw new IllegalArgumentException(
                    "No puedes editar tu propia cuenta desde esta sección.");
        }

        String usuarioLimpio = validarUsuario(usuario);
        String nombreLimpio = validarNombre(nombre);
        String correoLimpio = validarCorreo(correo);

        encargadoRepository.findByUsuarioIgnoreCase(usuarioLimpio)
                .filter(otraCuenta ->
                        !otraCuenta.getId().equals(encargado.getId()))
                .ifPresent(otraCuenta -> {
                    throw new IllegalArgumentException(
                            "Ese nombre de usuario ya está registrado.");
                });

        encargadoRepository.findByCorreoIgnoreCase(correoLimpio)
                .filter(otraCuenta ->
                        !otraCuenta.getId().equals(encargado.getId()))
                .ifPresent(otraCuenta -> {
                    throw new IllegalArgumentException(
                            "Ese correo ya está registrado en otra cuenta administrativa.");
                });

        if (cuentaPersonaRepository.existsByCorreoIgnoreCase(correoLimpio)) {
            throw new IllegalArgumentException(
                    "Ese correo ya está registrado en una cuenta de alumno.");
        }

        boolean cambioCorreo =
                encargado.getCorreo() == null
                || !encargado.getCorreo().equalsIgnoreCase(correoLimpio);

        encargado.setUsuario(usuarioLimpio);
        encargado.setNombre(nombreLimpio);
        encargado.setCorreo(correoLimpio);

        if (cambioCorreo) {

            encargado.setCorreoVerificado(false);
            encargado.setFechaActivacion(null);

            Encargado encargadoGuardado =
                    encargadoRepository.save(encargado);

            String token =
                    tokenCuentaService.crearTokenActivacion(encargadoGuardado);

            correoService.enviarActivacion(
                    correoLimpio,
                    nombreLimpio,
                    token);

            return encargadoGuardado;
        }

        return encargadoRepository.save(encargado);
    }

    // =====================================================
    // DAR DE BAJA UNA CUENTA ADMINISTRATIVA
    // =====================================================

    @Transactional
    public void desactivarCuenta(
            Long id,
            String usuarioSolicitante) {

        Encargado solicitante =
                validarAdministradorSolicitante(usuarioSolicitante);

        Encargado encargado = encargadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta que deseas desactivar no existe."));

        validarCuentaAdministrativa(encargado);

        if (encargado.getId().equals(solicitante.getId())) {
            throw new IllegalArgumentException(
                    "No puedes dar de baja tu propia cuenta.");
        }

        if (!encargado.isActivo()) {
            throw new IllegalArgumentException(
                    "Esta cuenta ya se encuentra inactiva.");
        }

        if ("ADMIN".equalsIgnoreCase(encargado.getRol())) {

            long administradoresActivos =
                    encargadoRepository
                            .findAllByRolIgnoreCaseAndActivoTrue("ADMIN")
                            .size();

            if (administradoresActivos <= 1) {
                throw new IllegalArgumentException(
                        "No puedes dar de baja al último administrador activo.");
            }
        }

        encargado.setActivo(false);

        encargadoRepository.save(encargado);
    }

    // =====================================================
    // REACTIVAR UNA CUENTA ADMINISTRATIVA
    // =====================================================

    @Transactional
    public Encargado reactivarCuenta(
            Long id,
            String usuarioSolicitante) {

        validarAdministradorSolicitante(usuarioSolicitante);

        Encargado encargado = encargadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta que deseas reactivar no existe."));

        validarCuentaAdministrativa(encargado);

        if (encargado.isActivo()) {
            throw new IllegalArgumentException(
                    "Esta cuenta ya se encuentra activa.");
        }

        encargado.setActivo(true);

        Encargado encargadoGuardado =
                encargadoRepository.save(encargado);

        if (!Boolean.TRUE.equals(encargadoGuardado.getCorreoVerificado())) {

            if (encargadoGuardado.getCorreo() == null
                    || encargadoGuardado.getCorreo().isBlank()) {

                throw new IllegalArgumentException(
                        "La cuenta no tiene un correo electrónico válido para su activación.");
            }

            String token =
                    tokenCuentaService.crearTokenActivacion(encargadoGuardado);

            correoService.enviarActivacion(
                    encargadoGuardado.getCorreo(),
                    encargadoGuardado.getNombre(),
                    token);
        }

        return encargadoGuardado;
    }

    // =====================================================
    // VALIDAR QUE SEA UNA CUENTA ADMINISTRATIVA
    // =====================================================

    private void validarCuentaAdministrativa(Encargado encargado) {

        if (!"ADMIN".equalsIgnoreCase(encargado.getRol())
                && !"ENCARGADO".equalsIgnoreCase(encargado.getRol())) {

            throw new IllegalArgumentException(
                    "La cuenta seleccionada no es una cuenta administrativa.");
        }
    }

    // =====================================================
    // VALIDACIONES DE DATOS
    // =====================================================

    private String validarUsuario(String usuario) {

        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de usuario es obligatorio.");
        }

        String limpio = usuario.trim();

        if (limpio.length() > 80) {
            throw new IllegalArgumentException(
                    "El nombre de usuario no puede superar los 80 caracteres.");
        }

        return limpio;
    }

    private String validarNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre completo es obligatorio.");
        }

        String limpio = nombre.trim();

        if (limpio.length() > 120) {
            throw new IllegalArgumentException(
                    "El nombre completo no puede superar los 120 caracteres.");
        }

        return limpio;
    }

    private String validarCorreo(String correo) {

        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo electrónico es obligatorio.");
        }

        String limpio = correo.trim().toLowerCase(Locale.ROOT);

        if (limpio.length() > 254) {
            throw new IllegalArgumentException(
                    "El correo electrónico es demasiado largo.");
        }

        if (!limpio.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {

            throw new IllegalArgumentException(
                    "Escribe un correo electrónico válido.");
        }

        return limpio;
    }

    // =====================================================
    // MÉTODO GENERAL PARA CREAR CUENTAS ADMINISTRATIVAS
    // =====================================================

    private Encargado crearCuentaAdministrativa(
            String usuario,
            String nombre,
            String correo,
            String rol) {

        String usuarioLimpio = validarUsuario(usuario);
        String nombreLimpio = validarNombre(nombre);
        String correoLimpio = validarCorreo(correo);

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

        String token =
                tokenCuentaService.crearTokenActivacion(encargadoGuardado);

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

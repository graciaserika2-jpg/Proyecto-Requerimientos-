
package com.proyectoRequerimientos.prestamo_equipos.service;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyectoRequerimientos.prestamo_equipos.model.Encargado;
import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;

@Service
public class EncargadoService {

    private final EncargadoRepository encargadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenCuentaService tokenCuentaService;
    private final CorreoService correoService;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String CARACTERES =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public EncargadoService(
            EncargadoRepository encargadoRepository,
            PasswordEncoder passwordEncoder,
            TokenCuentaService tokenCuentaService,
            CorreoService correoService) {

        this.encargadoRepository = encargadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenCuentaService = tokenCuentaService;
        this.correoService = correoService;
    }

    public List<Encargado> listarEncargados() {

        return encargadoRepository.findAll()
                .stream()
                .filter(Encargado::isActivo)
                .toList();
    }

    // =====================================================
    // REGISTRAR ENCARGADO Y ENVIAR ACTIVACIÓN
    // =====================================================

    @Transactional
    public Encargado crearEncargado(
            String usuario,
            String nombre,
            String correo) {

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
        String correoLimpio = correo.trim().toLowerCase();

        if (!correoLimpio.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {

            throw new IllegalArgumentException(
                    "Escribe un correo electrónico válido.");
        }

        if (encargadoRepository
                .findByUsuarioIgnoreCase(usuarioLimpio)
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Ese nombre de usuario ya está registrado.");
        }

        if (encargadoRepository
                .existsByCorreoIgnoreCase(correoLimpio)) {

            throw new IllegalArgumentException(
                    "Ese correo ya está registrado en otra cuenta.");
        }

        Encargado encargado = new Encargado();

        encargado.setUsuario(usuarioLimpio);
        encargado.setNombre(nombreLimpio);

        // Se guarda una contraseña aleatoria que nadie conoce.
        encargado.setPasswordHash(
                passwordEncoder.encode(generarPasswordAleatoria()));

        encargado.setRol("ENCARGADO");
        encargado.setActivo(true);
        encargado.setDebeCambiarPassword(true);

        encargado.setCorreo(correoLimpio);
        encargado.setCorreoVerificado(false);

        Encargado encargadoGuardado =
                encargadoRepository.save(encargado);

        // Crear enlace de activación.
        String token = tokenCuentaService
                .crearTokenActivacion(encargadoGuardado);

        // Enviar correo al encargado.
        correoService.enviarActivacion(
                correoLimpio,
                nombreLimpio,
                token
        );

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

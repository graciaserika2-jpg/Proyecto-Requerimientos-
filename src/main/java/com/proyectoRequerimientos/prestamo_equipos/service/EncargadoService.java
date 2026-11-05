
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

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String CARACTERES =
            "abcdefghijkmnopqrstuvwxyz23456789";

    public EncargadoService(
            EncargadoRepository encargadoRepository,
            PasswordEncoder passwordEncoder) {

        this.encargadoRepository = encargadoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Encargado> listarEncargados() {
        return encargadoRepository.findAll()
                .stream()
                .filter(Encargado::isActivo)
                .toList();
    }

    @Transactional
    public CredencialesTemporales crearEncargado(
            String usuario,
            String nombre) {

        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de usuario es obligatorio.");
        }

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre completo es obligatorio.");
        }

        String usuarioLimpio = usuario.trim();

        if (encargadoRepository
                .findByUsuarioIgnoreCase(usuarioLimpio)
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Ese nombre de usuario ya está registrado.");
        }

        String passwordTemporal = generarPasswordTemporal();

        Encargado encargado = new Encargado();

        encargado.setUsuario(usuarioLimpio);
        encargado.setNombre(nombre.trim());
        encargado.setPasswordHash(
                passwordEncoder.encode(passwordTemporal));
        encargado.setRol("ENCARGADO");
        encargado.setActivo(true);

        // El encargado deberá cambiar su contraseña al iniciar sesión.
        encargado.setDebeCambiarPassword(true);

        Encargado encargadoGuardado =
                encargadoRepository.save(encargado);

        return new CredencialesTemporales(
                encargadoGuardado.getId(),
                encargadoGuardado.getNombre(),
                encargadoGuardado.getUsuario(),
                encargadoGuardado.getRol(),
                passwordTemporal
        );
    }

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

    private String generarPasswordTemporal() {

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int posicion = RANDOM.nextInt(CARACTERES.length());

            password.append(CARACTERES.charAt(posicion));
        }

        return password.toString();
    }

    public record CredencialesTemporales(
            Long id,
            String nombre,
            String usuario,
            String rol,
            String passwordTemporal
    ) {
    }
}

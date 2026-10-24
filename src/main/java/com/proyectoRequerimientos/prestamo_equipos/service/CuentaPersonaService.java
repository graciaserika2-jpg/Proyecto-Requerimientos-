
package com.proyectoRequerimientos.prestamo_equipos.service;

import java.security.SecureRandom;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyectoRequerimientos.prestamo_equipos.model.CuentaPersona;
import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.PersonaRepository;

@Service
public class CuentaPersonaService {

    private final CuentaPersonaRepository cuentaRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String CARACTERES =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    public CuentaPersonaService(
            CuentaPersonaRepository cuentaRepository,
            PersonaRepository personaRepository,
            PasswordEncoder passwordEncoder) {
        this.cuentaRepository = cuentaRepository;
        this.personaRepository = personaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CredencialesTemporales crearCuenta(Long personaId) {

        if (personaId == null) {
            throw new IllegalArgumentException(
                    "El identificador de la persona es obligatorio.");
        }

        Persona persona = personaRepository.findById(personaId)
                .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "La persona no existe o está inactiva."));

        if (cuentaRepository.findByPersona_Id(personaId).isPresent()) {
            throw new IllegalArgumentException(
                    "Esta persona ya tiene una cuenta.");
        }

        String usuario = persona.getIdentificador()
                .trim().toLowerCase(Locale.ROOT);

        if (cuentaRepository.existsByUsuarioIgnoreCase(usuario)) {
            throw new IllegalArgumentException(
                    "El identificador ya está asociado a otra cuenta.");
        }

        String passwordTemporal = generarPasswordTemporal();

        CuentaPersona cuenta = new CuentaPersona();
        cuenta.setUsuario(usuario);
        cuenta.setPasswordHash(
                passwordEncoder.encode(passwordTemporal));
        cuenta.setPersona(persona);
        cuenta.setActivo(true);
        cuenta.setDebeCambiarPassword(true);

        cuentaRepository.save(cuenta);

        return new CredencialesTemporales(usuario, passwordTemporal);
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

        CuentaPersona cuenta = cuentaRepository
                .findByUsuarioIgnoreCase(usuario.trim())
                .filter(CuentaPersona::isActivo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta no existe o está inactiva."));

        if (!passwordEncoder.matches(
                passwordActual, cuenta.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "La contraseña actual es incorrecta.");
        }

        if (passwordEncoder.matches(
                passwordNueva, cuenta.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "La nueva contraseña debe ser diferente de la actual.");
        }

        cuenta.setPasswordHash(passwordEncoder.encode(passwordNueva));
        cuenta.setDebeCambiarPassword(false);

        cuentaRepository.save(cuenta);
    }

    private String generarPasswordTemporal() {
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 14; i++) {
            int indice = secureRandom.nextInt(CARACTERES.length());
            password.append(CARACTERES.charAt(indice));
        }

        return password.toString();
    }

    public record CredencialesTemporales(
            String usuario,
            String passwordTemporal) {
    }
}

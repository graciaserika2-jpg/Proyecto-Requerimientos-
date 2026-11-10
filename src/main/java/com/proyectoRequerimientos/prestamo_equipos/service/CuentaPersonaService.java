
package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.model.CuentaPersona;
import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.PersonaRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

@Service
public class CuentaPersonaService {

    private final CuentaPersonaRepository cuentaRepository;
    private final PersonaRepository personaRepository;
    private final EncargadoRepository encargadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenCuentaService tokenCuentaService;
    private final CorreoService correoService;

    private final SecureRandom secureRandom = new SecureRandom();

    public CuentaPersonaService(
            CuentaPersonaRepository cuentaRepository,
            PersonaRepository personaRepository,
            EncargadoRepository encargadoRepository,
            PasswordEncoder passwordEncoder,
            TokenCuentaService tokenCuentaService,
            CorreoService correoService) {

        this.cuentaRepository = cuentaRepository;
        this.personaRepository = personaRepository;
        this.encargadoRepository = encargadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenCuentaService = tokenCuentaService;
        this.correoService = correoService;
    }

    @Transactional
    public CuentaPersona crearCuenta(Long personaId, String correo) {

        if (personaId == null) {
            throw new IllegalArgumentException(
                    "El identificador de la persona es obligatorio.");
        }

        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo electrónico es obligatorio.");
        }

        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);

        if (!correoNormalizado.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {

            throw new IllegalArgumentException(
                    "El correo electrónico no tiene un formato válido.");
        }

        Persona persona = personaRepository.findById(personaId)
                .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "La persona no existe o está inactiva."));

        if (!"Alumno".equals(persona.getTipo())) {
            throw new IllegalArgumentException(
                    "Solamente se pueden crear cuentas nuevas para alumnos.");
        }

        if (cuentaRepository.existsByPersona_Id(personaId)) {
            throw new IllegalArgumentException(
                    "Esta persona ya tiene una cuenta registrada.");
        }

        String usuario = persona.getIdentificador()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (cuentaRepository.existsByUsuarioIgnoreCase(usuario)) {
            throw new IllegalArgumentException(
                    "Ya existe una cuenta con esa boleta.");
        }

        if (cuentaRepository.existsByCorreoIgnoreCase(correoNormalizado)
                || encargadoRepository.existsByCorreoIgnoreCase(correoNormalizado)) {

            throw new IllegalArgumentException(
                    "Ese correo electrónico ya está registrado en otra cuenta.");
        }

        CuentaPersona cuenta = new CuentaPersona();

        cuenta.setUsuario(usuario);
        cuenta.setPasswordHash(
                passwordEncoder.encode(generarPasswordAleatorio()));
        cuenta.setPersona(persona);
        cuenta.setCorreo(correoNormalizado);
        cuenta.setCorreoVerificado(false);
        cuenta.setFechaActivacion(null);
        cuenta.setActivo(true);
        cuenta.setDebeCambiarPassword(true);

        CuentaPersona cuentaGuardada = cuentaRepository.save(cuenta);

        String token = tokenCuentaService.crearTokenActivacion(
                cuentaGuardada);

        correoService.enviarActivacion(
                correoNormalizado,
                persona.getNombre(),
                token);

        return cuentaGuardada;
    }

    @Transactional
    public void cambiarPassword(
            String usuario,
            String passwordActual,
            String passwordNueva,
            String confirmarPassword) {

        if (passwordNueva == null
                || passwordNueva.length() < 12) {

            throw new IllegalArgumentException(
                    "La nueva contraseña debe tener al menos 12 caracteres.");
        }

        if (!passwordNueva.equals(confirmarPassword)) {
            throw new IllegalArgumentException(
                    "La confirmación de la contraseña no coincide.");
        }

        CuentaPersona cuenta = cuentaRepository
                .findByUsuarioIgnoreCase(usuario)
                .filter(CuentaPersona::isActivo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la cuenta."));

        if (!passwordEncoder.matches(
                passwordActual,
                cuenta.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "La contraseña actual es incorrecta.");
        }

        if (passwordEncoder.matches(
                passwordNueva,
                cuenta.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "La nueva contraseña debe ser diferente a la actual.");
        }

        cuenta.setPasswordHash(
                passwordEncoder.encode(passwordNueva));

        cuenta.setDebeCambiarPassword(false);

        cuentaRepository.save(cuenta);
    }

    private String generarPasswordAleatorio() {

        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}

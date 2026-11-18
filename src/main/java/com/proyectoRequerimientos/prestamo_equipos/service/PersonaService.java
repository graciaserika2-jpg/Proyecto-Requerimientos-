
package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.model.CuentaPersona;
import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import com.proyectoRequerimientos.prestamo_equipos.repository.PersonaRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final CuentaPersonaRepository cuentaPersonaRepository;
    private final CuentaPersonaService cuentaPersonaService;

    public PersonaService(
            PersonaRepository personaRepository,
            CuentaPersonaRepository cuentaPersonaRepository,
            CuentaPersonaService cuentaPersonaService) {

        this.personaRepository = personaRepository;
        this.cuentaPersonaRepository = cuentaPersonaRepository;
        this.cuentaPersonaService = cuentaPersonaService;
    }

    public List<Persona> listarPersonas() {
        return personaRepository.findByActivoTrueOrActivoIsNull();
    }

    public List<Persona> listarTodasLasPersonas() {
        return personaRepository.findAll()
                .stream()
                .filter(p -> "Alumno".equals(p.getTipo()))
                .toList();
    }

    public List<Persona> buscarParaPrestamo(String texto) {

        if (texto == null || texto.trim().length() < 2) {
            return List.of();
        }

        return personaRepository.buscarParaPrestamo(
                texto.trim(),
                PageRequest.of(0, 10)
        );
    }

    public Persona buscarActiva(Long id) {

        return personaRepository.findById(id)
                .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Persona no encontrada"));
    }

    @Transactional
    public Persona editarAlumno(
            Long id,
            String nombre,
            String identificador) {

        Persona persona = buscarPorId(id);

        validarDatosAlumno(nombre, identificador);

        String nombreNormalizado = nombre.trim();
        String boletaNormalizada = identificador.trim();

        boolean boletaDuplicada = personaRepository.findAll()
                .stream()
                .anyMatch(p ->
                        !p.getId().equals(id)
                        && p.getIdentificador() != null
                        && p.getIdentificador().equalsIgnoreCase(
                                boletaNormalizada));

        if (boletaDuplicada) {
            throw new IllegalArgumentException(
                    "Esa boleta ya está registrada en otro alumno.");
        }

        persona.setNombre(nombreNormalizado);
        persona.setIdentificador(boletaNormalizada);

        CuentaPersona cuenta = cuentaPersonaRepository
                .findByPersona_Id(id)
                .orElse(null);

        if (cuenta != null) {
            cuenta.setUsuario(
                    boletaNormalizada.toLowerCase(Locale.ROOT)
            );
            cuentaPersonaRepository.save(cuenta);
        }

        return personaRepository.save(persona);
    }

    @Transactional
    public void darDeBajaAlumno(Long id) {

        Persona persona = buscarPorId(id);

        if (Boolean.FALSE.equals(persona.getActivo())) {
            throw new IllegalArgumentException(
                    "El alumno ya se encuentra dado de baja.");
        }

        persona.setActivo(false);
        personaRepository.save(persona);

        cuentaPersonaRepository.findByPersona_Id(id)
                .ifPresent(cuenta -> {
                    cuenta.setActivo(false);
                    cuentaPersonaRepository.save(cuenta);
                });
    }

    @Transactional
    public void reactivarAlumno(Long id) {

        Persona persona = buscarPorId(id);

        if (!Boolean.FALSE.equals(persona.getActivo())) {
            throw new IllegalArgumentException(
                    "El alumno ya se encuentra activo.");
        }

        persona.setActivo(true);
        personaRepository.save(persona);

        cuentaPersonaRepository.findByPersona_Id(id)
                .ifPresent(cuenta -> {
                    cuenta.setActivo(true);
                    cuentaPersonaRepository.save(cuenta);
                });
    }

    private Persona buscarPorId(Long id) {

        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Alumno no encontrado."));

        if (!"Alumno".equals(persona.getTipo())) {
            throw new IllegalArgumentException(
                    "Esta operación solamente está disponible para alumnos.");
        }

        return persona;
    }

    // ==========================================
    // VALIDACIONES DE ALUMNOS
    // ==========================================

    private void validarNombreAlumno(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del alumno es obligatorio.");
        }

        String nombreNormalizado = nombre.trim();

        if (nombreNormalizado.length() > 150) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar los 150 caracteres.");
        }

        if (!nombreNormalizado.matches(
                "^[\\p{L}]+(?:\\s+[\\p{L}]+)*$")) {

            throw new IllegalArgumentException(
                    "El nombre solamente puede contener letras y espacios. " +
                    "No se permiten números ni símbolos.");
        }
    }

    private void validarDatosAlumno(
            String nombre,
            String identificador) {

        validarNombreAlumno(nombre);

        if (identificador == null
                || !identificador.trim().matches("[0-9]{10}")) {

            throw new IllegalArgumentException(
                    "La boleta debe contener exactamente 10 dígitos.");
        }
    }

    // ==========================================
    // REGISTRO COMPLETO DE ALUMNO Y CUENTA
    // ==========================================

    @Transactional
    public RegistroConCuenta registrarPersonaConCuenta(
            Persona persona,
            String correo) {

        validarAlumnoNuevo(persona);

        persona.setActivo(true);

        Persona personaGuardada = personaRepository.save(persona);

        CuentaPersona cuenta = cuentaPersonaService.crearCuenta(
                personaGuardada.getId(),
                correo
        );

        return new RegistroConCuenta(
                personaGuardada,
                cuenta.getCorreo(),
                cuenta.getCorreoVerificado()
        );
    }

    private void validarAlumnoNuevo(Persona persona) {

        if (persona == null) {
            throw new IllegalArgumentException(
                    "Los datos del alumno son obligatorios.");
        }

        validarNombreAlumno(persona.getNombre());

        if (!"Alumno".equals(persona.getTipo())) {
            throw new IllegalArgumentException(
                    "Solamente se pueden registrar alumnos nuevos.");
        }

        if (persona.getIdentificador() == null
                || !persona.getIdentificador().trim()
                .matches("[0-9]{10}")) {

            throw new IllegalArgumentException(
                    "La boleta debe contener exactamente 10 dígitos.");
        }

        persona.setNombre(persona.getNombre().trim());
        persona.setTipo("Alumno");
        persona.setIdentificador(
                persona.getIdentificador().trim());

        if (personaRepository.existsByIdentificadorIgnoreCase(
                persona.getIdentificador())) {

            throw new IllegalArgumentException(
                    "Esa boleta ya está registrada.");
        }
    }

    public record RegistroConCuenta(
            Persona persona,
            String correo,
            Boolean correoVerificado
    ) {
    }
}

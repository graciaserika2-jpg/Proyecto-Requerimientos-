
package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.model.CuentaPersona;
import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import com.proyectoRequerimientos.prestamo_equipos.repository.PersonaRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final CuentaPersonaService cuentaPersonaService;

    public PersonaService(
            PersonaRepository personaRepository,
            CuentaPersonaService cuentaPersonaService) {

        this.personaRepository = personaRepository;
        this.cuentaPersonaService = cuentaPersonaService;
    }

    public List<Persona> listarPersonas() {
        return personaRepository.findByActivoTrueOrActivoIsNull();
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

        if (persona.getNombre() == null
                || persona.getNombre().isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre del alumno es obligatorio.");
        }

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

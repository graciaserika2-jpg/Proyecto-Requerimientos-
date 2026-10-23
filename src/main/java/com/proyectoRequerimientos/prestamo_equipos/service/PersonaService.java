package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import com.proyectoRequerimientos.prestamo_equipos.repository.PersonaRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class PersonaService {

    private final PersonaRepository personaRepository;

    private static final Set<String> TIPOS =
            Set.of("Alumno", "Profesor");

    public PersonaService(PersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    public List<Persona> listarPersonas() {
        return personaRepository.findByActivoTrueOrActivoIsNull();
    }

    public Persona buscarActiva(Long id) {
        return personaRepository.findById(id)
                .filter(p -> !Boolean.FALSE.equals(p.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Persona no encontrada"));
    }

    public Persona guardarPersona(Persona persona) {

        if (persona == null) {
            throw new IllegalArgumentException(
                    "Los datos de la persona son obligatorios.");
        }

        if (persona.getNombre() == null
                || persona.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la persona es obligatorio.");
        }

        if (persona.getTipo() == null
                || !TIPOS.contains(persona.getTipo().trim())) {
            throw new IllegalArgumentException(
                    "El tipo debe ser Alumno o Profesor.");
        }

        if (persona.getIdentificador() == null
                || persona.getIdentificador().isBlank()) {
            throw new IllegalArgumentException(
                    "La boleta o el identificador institucional es obligatorio.");
        }

        String tipo = persona.getTipo().trim();
        String identificador = persona.getIdentificador().trim();

        // La boleta de los alumnos debe tener exactamente 10 dígitos.
        if ("Alumno".equals(tipo)
                && !identificador.matches("[0-9]{10}")) {
            throw new IllegalArgumentException(
                    "La boleta debe contener exactamente 10 dígitos, sin letras, espacios ni símbolos.");
        }

        // No imponemos un formato al identificador de profesores
        // hasta confirmar el formato institucional correspondiente.
        persona.setNombre(persona.getNombre().trim());
        persona.setTipo(tipo);
        persona.setIdentificador(identificador);

        boolean identificadorDuplicado =
                personaRepository.existsByIdentificadorIgnoreCase(
                        identificador);

        if (identificadorDuplicado) {
            throw new IllegalArgumentException(
                    "Ese identificador ya está registrado.");
        }

        persona.setActivo(true);

        return personaRepository.save(persona);
    }
}
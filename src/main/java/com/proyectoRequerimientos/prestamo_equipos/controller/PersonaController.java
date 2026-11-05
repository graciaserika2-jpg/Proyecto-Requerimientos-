package com.proyectoRequerimientos.prestamo_equipos.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;
import com.proyectoRequerimientos.prestamo_equipos.service.CuentaPersonaService;
import com.proyectoRequerimientos.prestamo_equipos.service.PersonaService;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final PersonaService personaService;
    private final CuentaPersonaService cuentaPersonaService;
    private final CuentaPersonaRepository cuentaPersonaRepository;

    public PersonaController(
            PersonaService personaService,
            CuentaPersonaService cuentaPersonaService,
            CuentaPersonaRepository cuentaPersonaRepository) {

        this.personaService = personaService;
        this.cuentaPersonaService = cuentaPersonaService;
        this.cuentaPersonaRepository = cuentaPersonaRepository;
    }

    @GetMapping
    public List<Persona> listarPersonas() {
        return personaService.listarPersonas();
    }

    @GetMapping("/estado-cuentas")
    public List<PersonaConCuenta> listarPersonasConEstadoCuenta() {

        return personaService.listarPersonas()
                .stream()
                .map(persona -> new PersonaConCuenta(
                        persona.getId(),
                        persona.getNombre(),
                        persona.getTipo(),
                        persona.getIdentificador(),
                        cuentaPersonaRepository.existsByPersona_Id(
                                persona.getId())
                ))
                .toList();
    }

    @GetMapping("/buscar")
    public List<Persona> buscarPersonas(
            @RequestParam String texto) {

        return personaService.buscarParaPrestamo(texto);
    }

    @PostMapping
    public Persona guardarPersona(
            @RequestBody Persona persona) {

        return personaService.guardarPersona(persona);
    }

    @PostMapping("/registro-completo")
    public PersonaService.RegistroConCuenta registrarPersonaConCuenta(
            @RequestBody Persona persona) {

        return personaService.registrarPersonaConCuenta(persona);
    }

    @PostMapping("/{id}/cuenta")
    public CuentaPersonaService.CredencialesTemporales crearCuenta(
            @PathVariable Long id) {

        return cuentaPersonaService.crearCuenta(id);
    }

    public record PersonaConCuenta(
            Long id,
            String nombre,
            String tipo,
            String identificador,
            boolean tieneCuenta
    ) {
    }
}
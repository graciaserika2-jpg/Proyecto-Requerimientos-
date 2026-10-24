
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
import com.proyectoRequerimientos.prestamo_equipos.service.CuentaPersonaService;
import com.proyectoRequerimientos.prestamo_equipos.service.PersonaService;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final PersonaService personaService;
    private final CuentaPersonaService cuentaPersonaService;

    public PersonaController(
            PersonaService personaService,
            CuentaPersonaService cuentaPersonaService) {
        this.personaService = personaService;
        this.cuentaPersonaService = cuentaPersonaService;
    }

    @GetMapping
    public List<Persona> listarPersonas() {
        return personaService.listarPersonas();
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

    @PostMapping("/{id}/cuenta")
    public CuentaPersonaService.CredencialesTemporales crearCuenta(
            @PathVariable Long id) {
        return cuentaPersonaService.crearCuenta(id);
    }
}

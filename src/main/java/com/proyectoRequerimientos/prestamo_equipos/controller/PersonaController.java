
package com.proyectoRequerimientos.prestamo_equipos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;
import com.proyectoRequerimientos.prestamo_equipos.service.PersonaService;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final PersonaService personaService;
    private final CuentaPersonaRepository cuentaPersonaRepository;

    public PersonaController(
            PersonaService personaService,
            CuentaPersonaRepository cuentaPersonaRepository) {

        this.personaService = personaService;
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
                .map(persona -> cuentaPersonaRepository
                        .findByPersona_Id(persona.getId())
                        .map(cuenta -> new PersonaConCuenta(
                                persona.getId(),
                                persona.getNombre(),
                                persona.getTipo(),
                                persona.getIdentificador(),
                                true,
                                cuenta.getCorreo(),
                                cuenta.getCorreoVerificado()
                        ))
                        .orElseGet(() -> new PersonaConCuenta(
                                persona.getId(),
                                persona.getNombre(),
                                persona.getTipo(),
                                persona.getIdentificador(),
                                false,
                                null,
                                null
                        )))
                .toList();
    }

    @GetMapping("/buscar")
    public List<Persona> buscarPersonas(
            @RequestParam String texto) {

        return personaService.buscarParaPrestamo(texto);
    }

    @PostMapping("/registro-completo")
    public ResponseEntity<?> registrarPersonaConCuenta(
            @RequestBody RegistroPersonaRequest solicitud) {

        try {
            Persona persona = new Persona();

            persona.setNombre(solicitud.nombre());
            persona.setTipo(solicitud.tipo());
            persona.setIdentificador(solicitud.identificador());

            PersonaService.RegistroConCuenta resultado =
                    personaService.registrarPersonaConCuenta(
                            persona,
                            solicitud.correo()
                    );

            return ResponseEntity.ok(resultado);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(new Respuesta(e.getMessage()));
        }
    }

    public record RegistroPersonaRequest(
            String nombre,
            String tipo,
            String identificador,
            String correo
    ) {
    }

    public record PersonaConCuenta(
            Long id,
            String nombre,
            String tipo,
            String identificador,
            boolean tieneCuenta,
            String correo,
            Boolean correoVerificado
    ) {
    }

    public record Respuesta(String mensaje) {
    }
}

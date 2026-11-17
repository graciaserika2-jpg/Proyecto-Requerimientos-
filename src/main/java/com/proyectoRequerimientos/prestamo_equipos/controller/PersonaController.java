
package com.proyectoRequerimientos.prestamo_equipos.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.proyectoRequerimientos.prestamo_equipos.model.BloqueoAdministrativo;
import com.proyectoRequerimientos.prestamo_equipos.model.CuentaPersona;
import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import com.proyectoRequerimientos.prestamo_equipos.repository.BloqueoAdministrativoRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;
import com.proyectoRequerimientos.prestamo_equipos.service.PersonaService;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final PersonaService personaService;
    private final CuentaPersonaRepository cuentaPersonaRepository;
    private final BloqueoAdministrativoRepository bloqueoRepository;

    public PersonaController(
            PersonaService personaService,
            CuentaPersonaRepository cuentaPersonaRepository,
            BloqueoAdministrativoRepository bloqueoRepository) {

        this.personaService = personaService;
        this.cuentaPersonaRepository = cuentaPersonaRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    @GetMapping
    public List<Persona> listarPersonas() {
        return personaService.listarPersonas();
    }

    // ==========================================
    // ESTADO DE LA PERSONA AUTENTICADA
    // ==========================================

    @GetMapping("/mi-estado")
    public MiEstadoPersona consultarMiEstado(
            Authentication authentication) {

        CuentaPersona cuenta = cuentaPersonaRepository
                .findByUsuarioIgnoreCase(authentication.getName())
                .filter(CuentaPersona::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una cuenta personal activa."));

        Persona persona = cuenta.getPersona();

        if (persona == null
                || Boolean.FALSE.equals(persona.getActivo())) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No se encontró una persona activa asociada a la cuenta.");
        }

        Optional<BloqueoAdministrativo> bloqueoActivo =
                bloqueoRepository
                        .findFirstByPersona_IdAndActivoTrueOrderByFechaBloqueoDesc(
                                persona.getId());

        boolean tieneBloqueoActivo = bloqueoActivo.isPresent();

        String motivoBloqueo = null;
        LocalDateTime fechaBloqueo = null;
        String equipoBloqueado = null;

        if (tieneBloqueoActivo) {

            BloqueoAdministrativo bloqueo = bloqueoActivo.get();

            motivoBloqueo = bloqueo.getMotivo();
            fechaBloqueo = bloqueo.getFechaBloqueo();

            if (bloqueo.getPrestamo() != null
                    && bloqueo.getPrestamo().getEquipo() != null) {

                equipoBloqueado = bloqueo.getPrestamo()
                        .getEquipo()
                        .getNombre();
            }
        }

        return new MiEstadoPersona(
                persona.getNombre(),
                persona.getTipo(),
                persona.getFechaFinSuspension(),
                tieneBloqueoActivo,
                motivoBloqueo,
                fechaBloqueo,
                equipoBloqueado
        );
    }

    // ==========================================
    // PERSONAS Y ESTADO DE SUS CUENTAS
    // ==========================================

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

    // ==========================================
    // REGISTRO COMPLETO DE PERSONA Y CUENTA
    // ==========================================

    @PostMapping("/registro-completo")
    public ResponseEntity<?> registrarPersonaConCuenta(
            @RequestBody RegistroPersonaRequest solicitud) {

        try {

            Persona persona = new Persona();

            persona.setNombre(solicitud.nombre());
            persona.setTipo("Alumno");
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

    // ==========================================
    // RESPUESTAS
    // ==========================================

    public record MiEstadoPersona(
            String nombre,
            String tipo,
            LocalDate fechaFinSuspension,
            boolean tieneBloqueoActivo,
            String motivoBloqueo,
            LocalDateTime fechaBloqueo,
            String equipoBloqueado
    ) {
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

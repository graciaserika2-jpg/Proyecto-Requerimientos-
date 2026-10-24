
package com.proyectoRequerimientos.prestamo_equipos.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyectoRequerimientos.prestamo_equipos.service.CuentaPersonaService;

@RestController
@RequestMapping("/api/mi-cuenta")
public class CuentaPersonaController {

    private final CuentaPersonaService cuentaPersonaService;

    public CuentaPersonaController(
            CuentaPersonaService cuentaPersonaService) {
        this.cuentaPersonaService = cuentaPersonaService;
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<?> cambiarPassword(
            @RequestBody SolicitudCambioPassword solicitud,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(new Respuesta("Debes iniciar sesión."));
        }

        try {
            cuentaPersonaService.cambiarPassword(
                    principal.getName(),
                    solicitud.passwordActual(),
                    solicitud.passwordNueva(),
                    solicitud.confirmarPassword());

            return ResponseEntity.ok(
                    new Respuesta("Contraseña actualizada correctamente."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new Respuesta(e.getMessage()));
        }
    }

    public record SolicitudCambioPassword(
            String passwordActual,
            String passwordNueva,
            String confirmarPassword) {
    }

    public record Respuesta(String mensaje) {
    }
}

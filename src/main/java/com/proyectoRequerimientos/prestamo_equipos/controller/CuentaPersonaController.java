
package com.proyectoRequerimientos.prestamo_equipos.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;
import com.proyectoRequerimientos.prestamo_equipos.service.CuentaPersonaService;
import com.proyectoRequerimientos.prestamo_equipos.service.EncargadoService;

@RestController
@RequestMapping("/api/mi-cuenta")
public class CuentaPersonaController {

    private final CuentaPersonaService cuentaPersonaService;
    private final EncargadoService encargadoService;
    private final EncargadoRepository encargadoRepository;

    public CuentaPersonaController(
            CuentaPersonaService cuentaPersonaService,
            EncargadoService encargadoService,
            EncargadoRepository encargadoRepository) {

        this.cuentaPersonaService = cuentaPersonaService;
        this.encargadoService = encargadoService;
        this.encargadoRepository = encargadoRepository;
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

            String usuario = principal.getName();

            if (encargadoRepository
                    .findByUsuarioIgnoreCase(usuario)
                    .isPresent()) {

                encargadoService.cambiarPassword(
                        usuario,
                        solicitud.passwordActual(),
                        solicitud.passwordNueva(),
                        solicitud.confirmarPassword());

            } else {

                cuentaPersonaService.cambiarPassword(
                        usuario,
                        solicitud.passwordActual(),
                        solicitud.passwordNueva(),
                        solicitud.confirmarPassword());
            }

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

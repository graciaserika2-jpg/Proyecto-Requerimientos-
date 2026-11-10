
package com.proyectoRequerimientos.prestamo_equipos.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;
import com.proyectoRequerimientos.prestamo_equipos.service.CuentaAccesoService;
import com.proyectoRequerimientos.prestamo_equipos.service.CuentaPersonaService;
import com.proyectoRequerimientos.prestamo_equipos.service.EncargadoService;

@RestController
@RequestMapping
public class CuentaPersonaController {

    private final CuentaPersonaService cuentaPersonaService;
    private final EncargadoService encargadoService;
    private final EncargadoRepository encargadoRepository;
    private final CuentaAccesoService cuentaAccesoService;

    public CuentaPersonaController(
            CuentaPersonaService cuentaPersonaService,
            EncargadoService encargadoService,
            EncargadoRepository encargadoRepository,
            CuentaAccesoService cuentaAccesoService) {

        this.cuentaPersonaService = cuentaPersonaService;
        this.encargadoService = encargadoService;
        this.encargadoRepository = encargadoRepository;
        this.cuentaAccesoService = cuentaAccesoService;
    }

    // CAMBIAR CONTRASEÑA CON SESIÓN INICIADA

    @PostMapping("/api/mi-cuenta/cambiar-password")
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

    // ACTIVAR CUENTA

    @PostMapping("/api/cuentas/activar")
    public ResponseEntity<Respuesta> activarCuenta(
            @RequestBody SolicitudPasswordToken solicitud) {

        try {

            cuentaAccesoService.activarCuenta(
                    solicitud.token(),
                    solicitud.password(),
                    solicitud.confirmarPassword());

            return ResponseEntity.ok(
                    new Respuesta("Tu cuenta ha sido activada correctamente."));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(new Respuesta(e.getMessage()));
        }
    }

    // SOLICITAR RECUPERACIÓN DE CONTRASEÑA

    @PostMapping("/api/cuentas/solicitar-recuperacion")
    public ResponseEntity<Respuesta> solicitarRecuperacion(
            @RequestBody SolicitudCorreo solicitud) {

        try {

            cuentaAccesoService.solicitarRecuperacion(
                    solicitud.correo());

            return ResponseEntity.ok(
                    new Respuesta(
                            "Si existe una cuenta activa y verificada con ese correo, recibirás instrucciones para recuperar tu contraseña."));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(new Respuesta(e.getMessage()));
        }
    }

    // RESTABLECER CONTRASEÑA

    @PostMapping("/api/cuentas/restablecer-password")
    public ResponseEntity<Respuesta> restablecerPassword(
            @RequestBody SolicitudPasswordToken solicitud) {

        try {

            cuentaAccesoService.restablecerPassword(
                    solicitud.token(),
                    solicitud.password(),
                    solicitud.confirmarPassword());

            return ResponseEntity.ok(
                    new Respuesta("Tu contraseña se ha restablecido correctamente."));

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

    public record SolicitudPasswordToken(
            String token,
            String password,
            String confirmarPassword) {
    }

    public record SolicitudCorreo(String correo) {
    }

    public record Respuesta(String mensaje) {
    }
}

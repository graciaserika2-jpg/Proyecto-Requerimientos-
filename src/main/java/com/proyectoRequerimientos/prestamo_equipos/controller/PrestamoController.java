
package com.proyectoRequerimientos.prestamo_equipos.controller;

import com.proyectoRequerimientos.prestamo_equipos.dto.CrearPrestamoRequest;
import com.proyectoRequerimientos.prestamo_equipos.dto.MiPrestamoResponse;
import com.proyectoRequerimientos.prestamo_equipos.model.BloqueoAdministrativo;
import com.proyectoRequerimientos.prestamo_equipos.model.Prestamo;
import com.proyectoRequerimientos.prestamo_equipos.service.BloqueoAdministrativoService;
import com.proyectoRequerimientos.prestamo_equipos.service.PrestamoService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    private final PrestamoService prestamoService;
    private final BloqueoAdministrativoService bloqueoService;

    public PrestamoController(
            PrestamoService prestamoService,
            BloqueoAdministrativoService bloqueoService) {

        this.prestamoService = prestamoService;
        this.bloqueoService = bloqueoService;
    }

    @GetMapping
    public List<PrestamoResponse> listarPrestamos() {

        return prestamoService.listarPrestamos()
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @GetMapping("/mi-historial")
    public List<MiPrestamoResponse> listarMiHistorial(
            Authentication authentication) {

        return prestamoService.listarHistorialUsuario(
                authentication.getName());
    }

    @PostMapping
    public PrestamoResponse crearPrestamo(
            @RequestBody CrearPrestamoRequest solicitud,
            Authentication authentication) {

        Prestamo prestamo = prestamoService.crearPrestamo(
                solicitud,
                authentication.getName()
        );

        return convertirRespuesta(prestamo);
    }

    @PostMapping("/{id}/devolver")
    public PrestamoResponse devolverPrestamo(
            @PathVariable Long id,
            @RequestBody DevolucionRequest solicitud,
            Authentication authentication) {

        Prestamo prestamo = prestamoService.devolverPrestamo(
                id,
                solicitud.condicionDevolucion(),
                solicitud.detallesDanio(),
                authentication.getName()
        );

        return convertirRespuesta(prestamo);
    }

    @PostMapping("/{id}/revision-mantenimiento")
    public PrestamoResponse registrarRevisionMantenimiento(
            @PathVariable Long id,
            @RequestBody RevisionMantenimientoRequest solicitud,
            Authentication authentication) {

        Prestamo prestamo =
                prestamoService.registrarRevisionMantenimiento(
                        id,
                        solicitud.revisionMantenimiento(),
                        authentication.getName()
                );

        return convertirRespuesta(prestamo);
    }

    // ==========================================
    // BLOQUEOS ADMINISTRATIVOS
    // ==========================================

    @GetMapping("/bloqueos/activos")
    public List<BloqueoResponse> listarBloqueosActivos() {

        return bloqueoService.listarBloqueosActivos()
                .stream()
                .map(this::convertirBloqueoRespuesta)
                .toList();
    }

    @GetMapping("/bloqueos/historial")
    public List<BloqueoResponse> listarHistorialBloqueos() {

        return bloqueoService.listarHistorialBloqueos()
                .stream()
                .map(this::convertirBloqueoRespuesta)
                .toList();
    }

    @PostMapping("/bloqueos/{id}/levantar")
    public BloqueoResponse levantarBloqueo(
            @PathVariable Long id,
            @RequestBody LevantarBloqueoRequest solicitud,
            Authentication authentication) {

        BloqueoAdministrativo bloqueo =
                bloqueoService.levantarBloqueo(
                        id,
                        solicitud.motivoLevantamiento(),
                        authentication.getName()
                );

        return convertirBloqueoRespuesta(bloqueo);
    }

    private BloqueoResponse convertirBloqueoRespuesta(
            BloqueoAdministrativo bloqueo) {

        PersonaBloqueoResumen persona =
                bloqueo.getPersona() == null
                        ? null
                        : new PersonaBloqueoResumen(
                                bloqueo.getPersona().getNombre(),
                                bloqueo.getPersona().getIdentificador()
                        );

        String equipo = null;

        if (bloqueo.getPrestamo() != null
                && bloqueo.getPrestamo().getEquipo() != null) {

            equipo = bloqueo.getPrestamo()
                    .getEquipo()
                    .getNombre();
        }

        return new BloqueoResponse(
                bloqueo.getId(),
                persona,
                equipo,
                bloqueo.getMotivo(),
                bloqueo.getFechaBloqueo(),
                bloqueo.getBloqueadoPor(),
                bloqueo.isActivo(),
                bloqueo.getFechaLevantamiento(),
                bloqueo.getLevantadoPor(),
                bloqueo.getMotivoLevantamiento()
        );
    }

    private PrestamoResponse convertirRespuesta(Prestamo prestamo) {

        PersonaResumen persona = prestamo.getPersona() == null
                ? null
                : new PersonaResumen(
                        prestamo.getPersona().getNombre(),
                        prestamo.getPersona().getTipo(),
                        prestamo.getPersona().getIdentificador()
                );

        EquipoResumen equipo = prestamo.getEquipo() == null
                ? null
                : new EquipoResumen(
                        prestamo.getEquipo().getId(),
                        prestamo.getEquipo().getNombre()
                );

        return new PrestamoResponse(
                prestamo.getId(),
                persona,
                equipo,
                prestamo.getFechaPrestamo(),
                prestamo.getFechaDevolucionPrevista(),
                prestamo.getFechaDevolucionReal(),
                prestamo.getEstado(),
                prestamo.getObservaciones(),
                prestamo.getEntregadoPor(),
                prestamo.getCondicionDevolucion(),
                prestamo.getDetallesDanio(),
                prestamo.getRecibidoPor(),
                prestamo.getRevisionMantenimiento(),
                prestamo.getFechaRevision(),
                prestamo.getRevisadoPor(),
                prestamo.getMantenimientoAtendido()
        );
    }

    public record DevolucionRequest(
            String condicionDevolucion,
            String detallesDanio
    ) {
    }

    public record RevisionMantenimientoRequest(
            String revisionMantenimiento
    ) {
    }

    public record LevantarBloqueoRequest(
            String motivoLevantamiento
    ) {
    }

    public record PrestamoResponse(
            Long id,
            PersonaResumen persona,
            EquipoResumen equipo,
            LocalDateTime fechaPrestamo,
            LocalDate fechaDevolucionPrevista,
            LocalDateTime fechaDevolucionReal,
            String estado,
            String observaciones,
            String entregadoPor,
            String condicionDevolucion,
            String detallesDanio,
            String recibidoPor,
            String revisionMantenimiento,
            LocalDateTime fechaRevision,
            String revisadoPor,
            Boolean mantenimientoAtendido
    ) {
    }

    public record PersonaResumen(
            String nombre,
            String tipo,
            String identificador
    ) {
    }

    public record EquipoResumen(
            Long id,
            String nombre
    ) {
    }

    public record BloqueoResponse(
            Long id,
            PersonaBloqueoResumen persona,
            String equipo,
            String motivo,
            LocalDateTime fechaBloqueo,
            String bloqueadoPor,
            boolean activo,
            LocalDateTime fechaLevantamiento,
            String levantadoPor,
            String motivoLevantamiento
    ) {
    }

    public record PersonaBloqueoResumen(
            String nombre,
            String identificador
    ) {
    }
}

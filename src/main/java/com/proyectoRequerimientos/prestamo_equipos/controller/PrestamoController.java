
package com.proyectoRequerimientos.prestamo_equipos.controller;

import com.proyectoRequerimientos.prestamo_equipos.dto.CrearPrestamoRequest;
import com.proyectoRequerimientos.prestamo_equipos.dto.MiPrestamoResponse;
import com.proyectoRequerimientos.prestamo_equipos.model.Prestamo;
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

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
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
            @RequestBody CrearPrestamoRequest solicitud) {

        Prestamo prestamo = prestamoService.crearPrestamo(solicitud);

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

    public record PrestamoResponse(
            Long id,
            PersonaResumen persona,
            EquipoResumen equipo,
            LocalDateTime fechaPrestamo,
            LocalDate fechaDevolucionPrevista,
            LocalDateTime fechaDevolucionReal,
            String estado,
            String observaciones,
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
}

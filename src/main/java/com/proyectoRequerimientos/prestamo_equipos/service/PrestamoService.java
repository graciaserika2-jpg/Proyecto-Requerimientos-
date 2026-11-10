
package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.dto.CrearPrestamoRequest;
import com.proyectoRequerimientos.prestamo_equipos.dto.MiPrestamoResponse;
import com.proyectoRequerimientos.prestamo_equipos.model.CuentaPersona;
import com.proyectoRequerimientos.prestamo_equipos.model.Equipo;
import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import com.proyectoRequerimientos.prestamo_equipos.model.Prestamo;
import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.EquipoRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.PrestamoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class PrestamoService {

    private static final ZoneId ZONA_HORARIA =
            ZoneId.of("America/Mexico_City");

    private final PrestamoRepository prestamoRepository;
    private final EquipoRepository equipoRepository;
    private final PersonaService personaService;
    private final CuentaPersonaRepository cuentaPersonaRepository;

    public PrestamoService(
            PrestamoRepository prestamoRepository,
            EquipoRepository equipoRepository,
            PersonaService personaService,
            CuentaPersonaRepository cuentaPersonaRepository) {

        this.prestamoRepository = prestamoRepository;
        this.equipoRepository = equipoRepository;
        this.personaService = personaService;
        this.cuentaPersonaRepository = cuentaPersonaRepository;
    }

    @Transactional
    public List<Prestamo> listarPrestamos() {
        actualizarPrestamosVencidos();
        return prestamoRepository.findAllByOrderByFechaPrestamoDesc();
    }

    @Transactional(readOnly = true)
    public List<MiPrestamoResponse> listarHistorialUsuario(String usuario) {

        if (usuario == null || usuario.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar la cuenta."
            );
        }

        CuentaPersona cuenta = cuentaPersonaRepository
                .findByUsuarioIgnoreCase(usuario.trim())
                .filter(CuentaPersona::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No se encontró una cuenta de persona activa."
                ));

        Persona persona = cuenta.getPersona();

        if (persona == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La cuenta no tiene una persona asociada."
            );
        }

        return prestamoRepository
                .findByPersona_IdOrderByFechaPrestamoDesc(persona.getId())
                .stream()
                .map(prestamo -> new MiPrestamoResponse(
                        prestamo.getId(),
                        prestamo.getEquipo().getNombre(),
                        prestamo.getEquipo().getTipo(),
                        prestamo.getFechaPrestamo(),
                        prestamo.getFechaDevolucionPrevista(),
                        prestamo.getFechaDevolucionReal(),
                        prestamo.getEstado(),
                        prestamo.getObservaciones()
                ))
                .toList();
    }

    /**
     * Detecta préstamos vencidos y aplica la suspensión a los alumnos.
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "America/Mexico_City")
    public void actualizarPrestamosVencidos() {

        LocalDate hoy = LocalDate.now(ZONA_HORARIA);

        List<Prestamo> prestamosVencidos =
                prestamoRepository
                        .findByEstadoAndFechaDevolucionPrevistaBefore(
                                "Activo", hoy
                        );

        for (Prestamo prestamo : prestamosVencidos) {

            prestamo.setEstado("Vencido");

            Persona persona = prestamo.getPersona();

            if ("Alumno".equalsIgnoreCase(persona.getTipo())) {

                LocalDate fechaInicio =
                        prestamo.getFechaDevolucionPrevista().plusDays(1);

                LocalDate fechaFin = fechaInicio.plusDays(7);

                LocalDate suspensionActual =
                        persona.getFechaFinSuspension();

                if (suspensionActual == null
                        || suspensionActual.isBefore(fechaFin)) {

                    persona.setFechaFinSuspension(fechaFin);
                }
            }
        }

        if (!prestamosVencidos.isEmpty()) {
            prestamoRepository.saveAll(prestamosVencidos);
        }
    }

    @Transactional
    public Prestamo crearPrestamo(CrearPrestamoRequest solicitud) {

        actualizarPrestamosVencidos();

        if (solicitud == null
                || solicitud.personaId() == null
                || solicitud.equipoId() == null) {
            throw new IllegalArgumentException(
                    "La persona y el artículo son obligatorios"
            );
        }

        Persona persona = personaService.buscarActiva(
                solicitud.personaId()
        );

        if ("Alumno".equalsIgnoreCase(persona.getTipo())) {

            LocalDate fechaFin = persona.getFechaFinSuspension();
            LocalDate hoy = LocalDate.now(ZONA_HORARIA);

            if (fechaFin != null && hoy.isBefore(fechaFin)) {
                throw new IllegalArgumentException(
                        "El alumno está suspendido. Podrá solicitar "
                        + "préstamos nuevamente a partir del "
                        + fechaFin + "."
                );
            }

            if (fechaFin != null && !hoy.isBefore(fechaFin)) {
                persona.setFechaFinSuspension(null);
            }
        }

        Equipo equipo = equipoRepository.findById(solicitud.equipoId())
                .filter(e -> !Boolean.FALSE.equals(e.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Artículo no encontrado"
                ));

        if (!"Disponible".equals(equipo.getEstado())) {
            throw new IllegalArgumentException(
                    "El artículo no está disponible para préstamo"
            );
        }

        if (prestamoRepository.existsByPersona_IdAndEstadoIn(
                persona.getId(), List.of("Activo", "Vencido"))) {
            throw new IllegalArgumentException(
                    "La persona tiene un préstamo pendiente. "
                    + "Debe devolverlo antes de solicitar otro."
            );
        }

        if (prestamoRepository.existsByEquipo_IdAndEstadoIn(
                equipo.getId(), List.of("Activo", "Vencido"))) {
            throw new IllegalArgumentException(
                    "El artículo ya tiene un préstamo pendiente."
            );
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_HORARIA);

        Prestamo prestamo = new Prestamo();
        prestamo.setPersona(persona);
        prestamo.setEquipo(equipo);
        prestamo.setFechaPrestamo(ahora);
        prestamo.setFechaDevolucionPrevista(ahora.toLocalDate());
        prestamo.setObservaciones(solicitud.observaciones());
        prestamo.setEstado("Activo");

        equipo.setEstado("Prestado");
        equipoRepository.save(equipo);

        return prestamoRepository.save(prestamo);
    }

    @Transactional
    public Prestamo devolverPrestamo(
            Long id,
            String condicionDevolucion,
            String detallesDanio,
            String recibidoPor) {

        actualizarPrestamosVencidos();

        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Préstamo no encontrado"
                ));

        if (!"Activo".equals(prestamo.getEstado())
                && !"Vencido".equals(prestamo.getEstado())) {
            throw new IllegalArgumentException(
                    "Este préstamo ya fue devuelto"
            );
        }

        if (!"BUEN_ESTADO".equals(condicionDevolucion)
                && !"CON_DANOS".equals(condicionDevolucion)) {
            throw new IllegalArgumentException(
                    "Selecciona una condición de devolución válida."
            );
        }

        if ("CON_DANOS".equals(condicionDevolucion)
                && (detallesDanio == null || detallesDanio.isBlank())) {
            throw new IllegalArgumentException(
                    "Debes describir los daños encontrados."
            );
        }

        if (recibidoPor == null || recibidoPor.isBlank()) {
            throw new IllegalArgumentException(
                    "No se pudo identificar quién recibió el equipo."
            );
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_HORARIA);

        prestamo.setEstado("Devuelto");
        prestamo.setFechaDevolucionReal(ahora);
        prestamo.setCondicionDevolucion(condicionDevolucion);
        prestamo.setDetallesDanio(
                "CON_DANOS".equals(condicionDevolucion)
                        ? detallesDanio.trim()
                        : null
        );
        prestamo.setRecibidoPor(recibidoPor);

        Equipo equipo = prestamo.getEquipo();

        if (!Boolean.FALSE.equals(equipo.getActivo())) {

            if ("CON_DANOS".equals(condicionDevolucion)) {
                equipo.setEstado("Mantenimiento");
            } else {
                equipo.setEstado("Disponible");
            }

            equipoRepository.save(equipo);
        }

        return prestamoRepository.save(prestamo);
    }

    /**
     * Registra la revisión de un reporte de daños.
     * El reporte original nunca se modifica.
     */
    @Transactional
    public Prestamo registrarRevisionMantenimiento(
            Long id,
            String revisionMantenimiento,
            String revisadoPor) {

        if (revisionMantenimiento == null
                || revisionMantenimiento.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes describir la revisión realizada."
            );
        }

        if (revisadoPor == null || revisadoPor.isBlank()) {
            throw new IllegalArgumentException(
                    "No se pudo identificar al encargado que revisó el equipo."
            );
        }

        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reporte de daño no encontrado."
                ));

        if (!"Devuelto".equals(prestamo.getEstado())
                || !"CON_DANOS".equals(
                        prestamo.getCondicionDevolucion())) {
            throw new IllegalArgumentException(
                    "Este préstamo no tiene un reporte de daños válido."
            );
        }

        if (Boolean.TRUE.equals(prestamo.getMantenimientoAtendido())) {
            throw new IllegalArgumentException(
                    "Este reporte ya fue atendido."
            );
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_HORARIA);

        prestamo.setRevisionMantenimiento(
                revisionMantenimiento.trim()
        );
        prestamo.setFechaRevision(ahora);
        prestamo.setRevisadoPor(revisadoPor);
        prestamo.setMantenimientoAtendido(true);

        prestamoRepository.save(prestamo);

        Equipo equipo = prestamo.getEquipo();

        // Revisamos si el mismo equipo todavía tiene
        // otros reportes de daños sin atender.
        boolean tieneDaniosPendientes = prestamoRepository.findAll()
                .stream()
                .anyMatch(otro ->
                        otro.getEquipo() != null
                        && otro.getEquipo().getId().equals(equipo.getId())
                        && "CON_DANOS".equals(
                                otro.getCondicionDevolucion())
                        && !Boolean.TRUE.equals(
                                otro.getMantenimientoAtendido())
                );

        // Solo queda disponible cuando todos sus reportes
        // de daños están atendidos.
        if (!tieneDaniosPendientes
                && !Boolean.FALSE.equals(equipo.getActivo())) {

            equipo.setEstado("Disponible");
            equipoRepository.save(equipo);
        }

        return prestamo;
    }
}

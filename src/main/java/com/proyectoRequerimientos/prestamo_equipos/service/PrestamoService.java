
package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.dto.CrearPrestamoRequest;
import com.proyectoRequerimientos.prestamo_equipos.dto.MiPrestamoResponse;
import com.proyectoRequerimientos.prestamo_equipos.model.BloqueoAdministrativo;
import com.proyectoRequerimientos.prestamo_equipos.model.CuentaPersona;
import com.proyectoRequerimientos.prestamo_equipos.model.Equipo;
import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import com.proyectoRequerimientos.prestamo_equipos.model.Prestamo;
import com.proyectoRequerimientos.prestamo_equipos.repository.BloqueoAdministrativoRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.EquipoRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.PrestamoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class PrestamoService {

    private static final ZoneId ZONA_HORARIA =
            ZoneId.of("America/Mexico_City");

    private static final LocalTime HORA_APERTURA =
            LocalTime.of(7, 0);

    private static final LocalTime HORA_CIERRE =
            LocalTime.of(22, 0);

    private final PrestamoRepository prestamoRepository;
    private final EquipoRepository equipoRepository;
    private final PersonaService personaService;
    private final CuentaPersonaRepository cuentaPersonaRepository;
    private final BloqueoAdministrativoRepository bloqueoAdministrativoRepository;

    public PrestamoService(
            PrestamoRepository prestamoRepository,
            EquipoRepository equipoRepository,
            PersonaService personaService,
            CuentaPersonaRepository cuentaPersonaRepository,
            BloqueoAdministrativoRepository bloqueoAdministrativoRepository) {

        this.prestamoRepository = prestamoRepository;
        this.equipoRepository = equipoRepository;
        this.personaService = personaService;
        this.cuentaPersonaRepository = cuentaPersonaRepository;
        this.bloqueoAdministrativoRepository =
                bloqueoAdministrativoRepository;
    }

    /**
     * Valida que las operaciones se realicen
     * dentro del horario de servicio.
     */
    private void validarHorarioOperacion() {

        LocalTime horaActual =
                LocalDateTime.now(ZONA_HORARIA).toLocalTime();

        if (horaActual.isBefore(HORA_APERTURA)
                || !horaActual.isBefore(HORA_CIERRE)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El horario para registrar préstamos y devoluciones "
                            + "es de 07:00 a 22:00 horas.");
        }
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
                    "No se pudo identificar la cuenta.");
        }

        CuentaPersona cuenta = cuentaPersonaRepository
                .findByUsuarioIgnoreCase(usuario.trim())
                .filter(CuentaPersona::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No se encontró una cuenta de alumno activa."));

        Persona persona = cuenta.getPersona();

        if (persona == null
                || !"Alumno".equalsIgnoreCase(persona.getTipo())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La cuenta no pertenece a un alumno.");
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
                        prestamo.getObservaciones()))
                .toList();
    }

    /**
     * Detecta préstamos vencidos.
     *
     * A las 22:00 cambia su estado a Vencido y genera
     * un bloqueo administrativo para los alumnos.
     *
     * También recupera vencimientos que ocurrieron
     * mientras el servidor estuvo apagado y crea
     * bloqueos faltantes para préstamos ya vencidos.
     */
    @Transactional
    @Scheduled(cron = "0 0 22 * * *", zone = "America/Mexico_City")
    @EventListener(ApplicationReadyEvent.class)
    public void actualizarPrestamosVencidos() {

        LocalDateTime ahora = LocalDateTime.now(ZONA_HORARIA);
        LocalDate hoy = ahora.toLocalDate();

        List<Prestamo> prestamosVencidos;

        if (ahora.toLocalTime().isBefore(HORA_CIERRE)) {

            prestamosVencidos = prestamoRepository
                    .findByEstadoAndFechaDevolucionPrevistaBefore(
                            "Activo", hoy);

        } else {

            prestamosVencidos = prestamoRepository
                    .findByEstadoAndFechaDevolucionPrevistaLessThanEqual(
                            "Activo", hoy);
        }

        // Procesamos los préstamos que acaban de vencer.
        for (Prestamo prestamo : prestamosVencidos) {

            prestamo.setEstado("Vencido");

            crearBloqueoSiCorresponde(prestamo, ahora);
        }

        if (!prestamosVencidos.isEmpty()) {
            prestamoRepository.saveAll(prestamosVencidos);
        }

        // Recuperamos también préstamos que ya estaban vencidos
        // antes de implementar este sistema de bloqueos.
        List<Prestamo> todosLosPrestamos = prestamoRepository.findAll();

        for (Prestamo prestamo : todosLosPrestamos) {

            if ("Vencido".equals(prestamo.getEstado())) {

                crearBloqueoSiCorresponde(prestamo, ahora);
            }
        }
    }

    /**
     * Crea un bloqueo administrativo una sola vez
     * por cada préstamo vencido de un alumno.
     *
     * Los registros históricos de profesores se conservan,
     * pero no generan bloqueos.
     */
    private void crearBloqueoSiCorresponde(
            Prestamo prestamo,
            LocalDateTime fechaBloqueo) {

        Persona persona = prestamo.getPersona();

        if (persona == null
                || !"Alumno".equalsIgnoreCase(persona.getTipo())) {
            return;
        }

        if (bloqueoAdministrativoRepository
                .existsByPrestamo_Id(prestamo.getId())) {
            return;
        }

        BloqueoAdministrativo bloqueo =
                new BloqueoAdministrativo();

        bloqueo.setPersona(persona);
        bloqueo.setPrestamo(prestamo);

        bloqueo.setMotivo(
                "El alumno no devolvió el equipo antes de las 22:00 "
                        + "horas de la fecha de devolución prevista.");

        bloqueo.setFechaBloqueo(fechaBloqueo);
        bloqueo.setBloqueadoPor("SISTEMA");
        bloqueo.setActivo(true);

        bloqueoAdministrativoRepository.save(bloqueo);
    }

    @Transactional
    public Prestamo crearPrestamo(
            CrearPrestamoRequest solicitud,
            String entregadoPor) {

        actualizarPrestamosVencidos();

        // Los préstamos nuevos siguen sujetos al horario de servicio.
        validarHorarioOperacion();

        if (entregadoPor == null || entregadoPor.isBlank()) {
            throw new IllegalArgumentException(
                    "No se pudo identificar quién entregó el equipo.");
        }

        if (solicitud == null
                || solicitud.personaId() == null
                || solicitud.equipoId() == null) {

            throw new IllegalArgumentException(
                    "El alumno y el artículo son obligatorios.");
        }

        Persona persona = personaService.buscarActiva(
                solicitud.personaId());

        // No se permiten préstamos para registros históricos
        // que todavía tengan el tipo Profesor.
        if (!"Alumno".equalsIgnoreCase(persona.getTipo())) {

            throw new IllegalArgumentException(
                    "Solo se pueden registrar préstamos a alumnos.");
        }

        // Primero se comprueba el bloqueo administrativo.
        boolean tieneBloqueoActivo =
                bloqueoAdministrativoRepository
                        .existsByPersona_IdAndActivoTrue(
                                persona.getId());

        if (tieneBloqueoActivo) {

            throw new IllegalArgumentException(
                    "El alumno tiene un bloqueo administrativo activo. "
                            + "Debe acudir con ADMIN o ENCARGADO para "
                            + "resolver su situación antes de solicitar "
                            + "otro préstamo.");
        }

        // Después se comprueba la suspensión temporal.
        LocalDate fechaFin = persona.getFechaFinSuspension();
        LocalDate hoy = LocalDate.now(ZONA_HORARIA);

        if (fechaFin != null && hoy.isBefore(fechaFin)) {

            throw new IllegalArgumentException(
                    "El alumno tiene una suspensión temporal. "
                            + "Podrá solicitar préstamos nuevamente "
                            + "a partir del " + fechaFin + ".");
        }

        if (fechaFin != null && !hoy.isBefore(fechaFin)) {
            persona.setFechaFinSuspension(null);
        }

        Equipo equipo = equipoRepository.findById(solicitud.equipoId())
                .filter(e -> !Boolean.FALSE.equals(e.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Artículo no encontrado"));

        if (!"Disponible".equals(equipo.getEstado())) {

            throw new IllegalArgumentException(
                    "El artículo no está disponible para préstamo");
        }

        if (prestamoRepository.existsByPersona_IdAndEstadoIn(
                persona.getId(), List.of("Activo", "Vencido"))) {

            throw new IllegalArgumentException(
                    "El alumno tiene un préstamo pendiente. "
                            + "Debe devolverlo antes de solicitar otro.");
        }

        if (prestamoRepository.existsByEquipo_IdAndEstadoIn(
                equipo.getId(), List.of("Activo", "Vencido"))) {

            throw new IllegalArgumentException(
                    "El artículo ya tiene un préstamo pendiente.");
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_HORARIA);

        Prestamo prestamo = new Prestamo();

        prestamo.setPersona(persona);
        prestamo.setEquipo(equipo);
        prestamo.setFechaPrestamo(ahora);
        prestamo.setFechaDevolucionPrevista(ahora.toLocalDate());
        prestamo.setObservaciones(solicitud.observaciones());
        prestamo.setEntregadoPor(entregadoPor);
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

        // Actualizamos los vencimientos antes de procesar la devolución.
        actualizarPrestamosVencidos();

        // IMPORTANTE:
        // Las devoluciones pueden registrarse a cualquier hora.
        // No se valida el horario en este método.

        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Préstamo no encontrado"));

        if (!"Activo".equals(prestamo.getEstado())
                && !"Vencido".equals(prestamo.getEstado())) {

            throw new IllegalArgumentException(
                    "Este préstamo ya fue devuelto");
        }

        if (!"BUEN_ESTADO".equals(condicionDevolucion)
                && !"CON_DANOS".equals(condicionDevolucion)) {

            throw new IllegalArgumentException(
                    "Selecciona una condición de devolución válida.");
        }

        if ("CON_DANOS".equals(condicionDevolucion)
                && (detallesDanio == null || detallesDanio.isBlank())) {

            throw new IllegalArgumentException(
                    "Debes describir los daños encontrados.");
        }

        if (recibidoPor == null || recibidoPor.isBlank()) {

            throw new IllegalArgumentException(
                    "No se pudo identificar quién recibió el equipo.");
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_HORARIA);

        // La devolución es tardía si el préstamo ya fue marcado
        // como Vencido o si ya pasó su fecha prevista.
        boolean devolucionTardia =
                "Vencido".equals(prestamo.getEstado())
                        || ahora.toLocalDate().isAfter(
                                prestamo.getFechaDevolucionPrevista());

        Persona persona = prestamo.getPersona();

        // La suspensión temporal aplica exclusivamente a alumnos.
        // Comienza el mismo día en que se registra la devolución tardía.
        if (devolucionTardia
                && persona != null
                && "Alumno".equalsIgnoreCase(persona.getTipo())) {

            // La fecha final es exclusiva:
            // se permite solicitar préstamos nuevamente ese día,
            // siempre que no exista un bloqueo administrativo activo.
            LocalDate fechaFinSuspension =
                    ahora.toLocalDate().plusDays(7);

            LocalDate suspensionActual =
                    persona.getFechaFinSuspension();

            // Si ya tenía una suspensión más larga,
            // conservamos la fecha que termine más tarde.
            if (suspensionActual == null
                    || suspensionActual.isBefore(fechaFinSuspension)) {

                persona.setFechaFinSuspension(
                        fechaFinSuspension);
            }
        }

        prestamo.setEstado("Devuelto");
        prestamo.setFechaDevolucionReal(ahora);
        prestamo.setCondicionDevolucion(condicionDevolucion);

        prestamo.setDetallesDanio(
                "CON_DANOS".equals(condicionDevolucion)
                        ? detallesDanio.trim()
                        : null);

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

        // Importante: devolver el equipo NO levanta
        // el bloqueo administrativo.
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
                    "Debes describir la revisión realizada.");
        }

        if (revisadoPor == null || revisadoPor.isBlank()) {

            throw new IllegalArgumentException(
                    "No se pudo identificar al encargado que revisó el equipo.");
        }

        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reporte de daño no encontrado."));

        if (!"Devuelto".equals(prestamo.getEstado())
                || !"CON_DANOS".equals(
                        prestamo.getCondicionDevolucion())) {

            throw new IllegalArgumentException(
                    "Este préstamo no tiene un reporte de daños válido.");
        }

        if (Boolean.TRUE.equals(prestamo.getMantenimientoAtendido())) {

            throw new IllegalArgumentException(
                    "Este reporte ya fue atendido.");
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_HORARIA);

        prestamo.setRevisionMantenimiento(
                revisionMantenimiento.trim());

        prestamo.setFechaRevision(ahora);
        prestamo.setRevisadoPor(revisadoPor);
        prestamo.setMantenimientoAtendido(true);

        prestamoRepository.save(prestamo);

        Equipo equipo = prestamo.getEquipo();

        // Revisamos si el mismo equipo todavía tiene
        // otros reportes de daños sin atender.
        boolean tieneDaniosPendientes = prestamoRepository.findAll()
                .stream()
                .anyMatch(otro -> otro.getEquipo() != null
                        && otro.getEquipo().getId().equals(equipo.getId())
                        && "CON_DANOS".equals(
                                otro.getCondicionDevolucion())
                        && !Boolean.TRUE.equals(
                                otro.getMantenimientoAtendido()));

        // Solo queda disponible cuando todos sus reportes
        // de daños están atendidos.
        if (!tieneDaniosPendientes
                && !Boolean.FALSE.equals(equipo.getActivo())) {

            equipo.setEstado("Disponible");
            equipoRepository.save(equipo);
        }

        return prestamoRepository.save(prestamo);
    }
}

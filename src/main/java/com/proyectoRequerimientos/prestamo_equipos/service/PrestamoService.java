
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PrestamoService {

    private static final LocalTime HORA_APERTURA = LocalTime.of(8, 0);
    private static final LocalTime HORA_CIERRE = LocalTime.of(20, 0);

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

    private void validarHorarioPrestamo() {
        LocalTime horaActual = LocalTime.now();

        if (horaActual.isBefore(HORA_APERTURA)
                || !horaActual.isBefore(HORA_CIERRE)) {
            throw new IllegalArgumentException(
                    "Solo se pueden registrar préstamos entre las 08:00 y las 20:00");
        }
    }

    public List<Prestamo> listarPrestamos() {
        return prestamoRepository.findAllByOrderByFechaPrestamoDesc();
    }

    /**
     * Obtiene exclusivamente los préstamos asociados
     * a la cuenta que inició sesión.
     */
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
                        "No se encontró una cuenta de persona activa."));

        Persona persona = cuenta.getPersona();

        if (persona == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La cuenta no tiene una persona asociada.");
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

    @Transactional
    public Prestamo crearPrestamo(CrearPrestamoRequest solicitud) {

        if (solicitud == null
                || solicitud.personaId() == null
                || solicitud.equipoId() == null) {
            throw new IllegalArgumentException(
                    "La persona y el artículo son obligatorios");
        }

        validarHorarioPrestamo();

        Persona persona = personaService.buscarActiva(solicitud.personaId());

        Equipo equipo = equipoRepository.findById(solicitud.equipoId())
                .filter(e -> !Boolean.FALSE.equals(e.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Artículo no encontrado"));

        if (!"Disponible".equals(equipo.getEstado())) {
            throw new IllegalArgumentException(
                    "El artículo no está disponible para préstamo");
        }

        if (prestamoRepository.existsByEquipo_IdAndEstado(
                equipo.getId(), "Activo")) {
            throw new IllegalArgumentException(
                    "El artículo ya tiene un préstamo activo");
        }

        if ("Alumno".equals(persona.getTipo())
                && prestamoRepository.existsByPersona_IdAndEstado(
                        persona.getId(), "Activo")) {
            throw new IllegalArgumentException(
                    "El alumno ya tiene un préstamo activo");
        }

        LocalDateTime ahora = LocalDateTime.now();

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
    public Prestamo devolverPrestamo(Long id) {

        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Préstamo no encontrado"));

        if (!"Activo".equals(prestamo.getEstado())) {
            throw new IllegalArgumentException(
                    "Este préstamo ya fue devuelto");
        }

        LocalDateTime ahora = LocalDateTime.now();

        if (!ahora.toLocalDate().equals(
                prestamo.getFechaPrestamo().toLocalDate())) {
            throw new IllegalArgumentException(
                    "El equipo debe devolverse el mismo día del préstamo");
        }

        if (!ahora.toLocalTime().isBefore(HORA_CIERRE)) {
            throw new IllegalArgumentException(
                    "El horario de devolución termina a las 20:00");
        }

        prestamo.setEstado("Devuelto");
        prestamo.setFechaDevolucionReal(ahora);

        Equipo equipo = prestamo.getEquipo();

        if (!Boolean.FALSE.equals(equipo.getActivo())) {
            equipo.setEstado("Disponible");
            equipoRepository.save(equipo);
        }

        return prestamoRepository.save(prestamo);
    }
}

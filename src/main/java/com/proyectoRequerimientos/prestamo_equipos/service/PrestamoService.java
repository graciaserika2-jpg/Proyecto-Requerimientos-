
package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.dto.CrearPrestamoRequest;
import com.proyectoRequerimientos.prestamo_equipos.model.Equipo;
import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import com.proyectoRequerimientos.prestamo_equipos.model.Prestamo;
import com.proyectoRequerimientos.prestamo_equipos.repository.EquipoRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.PrestamoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final EquipoRepository equipoRepository;
    private final PersonaService personaService;

    public PrestamoService(
            PrestamoRepository prestamoRepository,
            EquipoRepository equipoRepository,
            PersonaService personaService) {
        this.prestamoRepository = prestamoRepository;
        this.equipoRepository = equipoRepository;
        this.personaService = personaService;
    }

    public List<Prestamo> listarPrestamos() {
        return prestamoRepository.findAllByOrderByFechaPrestamoDesc();
    }

    @Transactional
    public Prestamo crearPrestamo(CrearPrestamoRequest solicitud) {
        if (solicitud == null
                || solicitud.personaId() == null
                || solicitud.equipoId() == null
                || solicitud.fechaDevolucionPrevista() == null) {
            throw new IllegalArgumentException(
                    "Persona, artículo y fecha prevista son obligatorios");
        }

        if (solicitud.fechaDevolucionPrevista().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha prevista no puede estar en el pasado");
        }

        Persona persona = personaService.buscarActiva(
                solicitud.personaId());

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

        Prestamo prestamo = new Prestamo();
        prestamo.setPersona(persona);
        prestamo.setEquipo(equipo);
        prestamo.setFechaPrestamo(LocalDateTime.now());
        prestamo.setFechaDevolucionPrevista(
                solicitud.fechaDevolucionPrevista());
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

        prestamo.setEstado("Devuelto");
        prestamo.setFechaDevolucionReal(LocalDateTime.now());

        Equipo equipo = prestamo.getEquipo();

        // Si el artículo sigue activo, vuelve a estar disponible.
        if (!Boolean.FALSE.equals(equipo.getActivo())) {
            equipo.setEstado("Disponible");
            equipoRepository.save(equipo);
        }

        return prestamoRepository.save(prestamo);
    }
}

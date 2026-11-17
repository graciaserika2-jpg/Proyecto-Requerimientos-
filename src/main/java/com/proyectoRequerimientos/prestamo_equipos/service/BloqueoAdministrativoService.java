
package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.model.BloqueoAdministrativo;
import com.proyectoRequerimientos.prestamo_equipos.repository.BloqueoAdministrativoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class BloqueoAdministrativoService {

    private static final ZoneId ZONA_HORARIA =
            ZoneId.of("America/Mexico_City");

    private final BloqueoAdministrativoRepository bloqueoRepository;

    public BloqueoAdministrativoService(
            BloqueoAdministrativoRepository bloqueoRepository) {

        this.bloqueoRepository = bloqueoRepository;
    }

    /**
     * Consulta todos los bloqueos administrativos activos.
     */
    @Transactional(readOnly = true)
    public List<BloqueoAdministrativo> listarBloqueosActivos() {

        return bloqueoRepository
                .findByActivoTrueOrderByFechaBloqueoDesc();
    }

    /**
     * Consulta el historial completo de bloqueos,
     * tanto activos como levantados.
     */
    @Transactional(readOnly = true)
    public List<BloqueoAdministrativo> listarHistorialBloqueos() {

        return bloqueoRepository
                .findAll()
                .stream()
                .sorted((a, b) ->
                        b.getFechaBloqueo().compareTo(a.getFechaBloqueo()))
                .toList();
    }

    /**
     * Levanta manualmente un bloqueo administrativo.
     *
     * Conserva el registro en la base de datos y guarda:
     * - Fecha de levantamiento.
     * - Usuario que levantó el bloqueo.
     * - Motivo del levantamiento.
     */
    @Transactional
    public BloqueoAdministrativo levantarBloqueo(
            Long id,
            String motivoLevantamiento,
            String levantadoPor) {

        if (motivoLevantamiento == null
                || motivoLevantamiento.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar el motivo para levantar el bloqueo.");
        }

        if (levantadoPor == null || levantadoPor.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar al usuario que realiza la operación.");
        }

        BloqueoAdministrativo bloqueo = bloqueoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el bloqueo administrativo."));

        if (!bloqueo.isActivo()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este bloqueo administrativo ya fue levantado.");
        }

        bloqueo.setActivo(false);
        bloqueo.setFechaLevantamiento(
                LocalDateTime.now(ZONA_HORARIA));
        bloqueo.setLevantadoPor(levantadoPor.trim());
        bloqueo.setMotivoLevantamiento(
                motivoLevantamiento.trim());

        return bloqueoRepository.save(bloqueo);
    }
}

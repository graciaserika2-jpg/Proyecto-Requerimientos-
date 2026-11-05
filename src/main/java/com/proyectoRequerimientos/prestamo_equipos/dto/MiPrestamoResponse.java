package com.proyectoRequerimientos.prestamo_equipos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MiPrestamoResponse(
        Long id,
        String equipoNombre,
        String equipoTipo,
        LocalDateTime fechaPrestamo,
        LocalDate fechaDevolucionPrevista,
        LocalDateTime fechaDevolucionReal,
        String estado,
        String observaciones
) {
}
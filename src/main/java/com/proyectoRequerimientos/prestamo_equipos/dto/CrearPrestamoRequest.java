package com.proyectoRequerimientos.prestamo_equipos.dto;

import java.time.LocalDate;

public record CrearPrestamoRequest(
        Long personaId,
        Long equipoId,
        LocalDate fechaDevolucionPrevista,
        String observaciones
) {
}
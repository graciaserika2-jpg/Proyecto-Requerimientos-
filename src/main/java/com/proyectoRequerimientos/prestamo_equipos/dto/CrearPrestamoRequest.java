package com.proyectoRequerimientos.prestamo_equipos.dto;

public record CrearPrestamoRequest(
        Long personaId,
        Long equipoId,
        String observaciones
) {
}

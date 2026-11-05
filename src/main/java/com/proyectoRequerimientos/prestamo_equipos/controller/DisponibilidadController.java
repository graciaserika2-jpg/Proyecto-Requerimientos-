package com.proyectoRequerimientos.prestamo_equipos.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyectoRequerimientos.prestamo_equipos.dto.EquipoDisponibilidad;
import com.proyectoRequerimientos.prestamo_equipos.service.EquipoService;

@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadController {

    private final EquipoService equipoService;

    public DisponibilidadController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping("/equipos")
    public List<EquipoDisponibilidad> listarEquiposDisponibles() {
        return equipoService.listarDisponibilidad();
    }
}
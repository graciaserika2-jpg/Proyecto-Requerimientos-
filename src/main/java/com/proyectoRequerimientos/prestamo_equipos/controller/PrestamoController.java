
package com.proyectoRequerimientos.prestamo_equipos.controller;

import com.proyectoRequerimientos.prestamo_equipos.dto.CrearPrestamoRequest;
import com.proyectoRequerimientos.prestamo_equipos.model.Prestamo;
import com.proyectoRequerimientos.prestamo_equipos.service.PrestamoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @GetMapping
    public List<Prestamo> listarPrestamos() {
        return prestamoService.listarPrestamos();
    }

    @PostMapping
    public Prestamo crearPrestamo(
            @RequestBody CrearPrestamoRequest solicitud) {
        return prestamoService.crearPrestamo(solicitud);
    }

    @PostMapping("/{id}/devolver")
    public Prestamo devolverPrestamo(@PathVariable Long id) {
        return prestamoService.devolverPrestamo(id);
    }
}

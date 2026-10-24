
package com.proyectoRequerimientos.prestamo_equipos.controller;
import org.springframework.web.bind.annotation.RequestParam;
import com.proyectoRequerimientos.prestamo_equipos.model.Equipo;
import com.proyectoRequerimientos.prestamo_equipos.service.EquipoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping
    public List<Equipo> listarEquipos() {
        return equipoService.listarEquipos();
    }

    @GetMapping("/buscar")
    public List<Equipo> buscarEquipos(
            @RequestParam String texto) {
        return equipoService.buscarDisponiblesParaPrestamo(texto);
    }

    @PostMapping
    public Equipo guardarEquipo(@RequestBody Equipo equipo) {
        return equipoService.guardarEquipo(equipo);
    }

    @PutMapping("/{id}")
    public Equipo actualizarEquipo(
            @PathVariable Long id,
            @RequestBody Equipo equipo) {

        return equipoService.actualizarEquipo(id, equipo);
    }

    @DeleteMapping("/{id}")
    public void eliminarEquipo(@PathVariable Long id) {
        equipoService.eliminarEquipo(id);
    }

}


package com.proyectoRequerimientos.prestamo_equipos.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.proyectoRequerimientos.prestamo_equipos.dto.CrearEncargadoRequest;
import com.proyectoRequerimientos.prestamo_equipos.model.Encargado;
import com.proyectoRequerimientos.prestamo_equipos.service.EncargadoService;

@RestController
@RequestMapping("/api/encargados")
public class EncargadoController {

    private final EncargadoService encargadoService;

    public EncargadoController(EncargadoService encargadoService) {
        this.encargadoService = encargadoService;
    }

    @GetMapping
    public List<Encargado> listarEncargados() {
        return encargadoService.listarEncargados();
    }

    @PostMapping
    public Map<String, Object> crearEncargado(
            @RequestBody CrearEncargadoRequest solicitud) {

        if (solicitud == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes enviar los datos del encargado.");
        }

        Encargado encargado = encargadoService.crearEncargado(
                solicitud.usuario(),
                solicitud.nombre(),
                solicitud.password()
        );

        return Map.of(
                "mensaje", "Encargado registrado correctamente.",
                "id", encargado.getId(),
                "usuario", encargado.getUsuario(),
                "nombre", encargado.getNombre(),
                "rol", encargado.getRol()
        );
    }
}

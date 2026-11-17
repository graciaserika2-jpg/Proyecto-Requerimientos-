
package com.proyectoRequerimientos.prestamo_equipos.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.proyectoRequerimientos.prestamo_equipos.model.Encargado;
import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;
import com.proyectoRequerimientos.prestamo_equipos.service.EncargadoService;

@RestController
@RequestMapping("/api/configuracion-inicial")
public class ConfiguracionInicialController {

    private final EncargadoService encargadoService;
    private final EncargadoRepository encargadoRepository;

    public ConfiguracionInicialController(
            EncargadoService encargadoService,
            EncargadoRepository encargadoRepository) {

        this.encargadoService = encargadoService;
        this.encargadoRepository = encargadoRepository;
    }

    // =====================================================
    // CONSULTAR SI YA EXISTE UN ADMINISTRADOR
    // =====================================================

    @GetMapping("/estado")
    public Map<String, Boolean> consultarEstado() {

        boolean configurado =
                encargadoRepository.existsByRolIgnoreCase("ADMIN");

        return Map.of("configurado", configurado);
    }

    // =====================================================
    // REGISTRAR EL PRIMER ADMINISTRADOR
    // =====================================================

    @PostMapping
    public Map<String, Object> crearPrimerAdministrador(
            @RequestBody CrearAdministradorRequest solicitud) {

        if (solicitud == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes proporcionar los datos del administrador.");
        }

        try {

            Encargado administrador =
                    encargadoService.crearPrimerAdministrador(
                            solicitud.usuario(),
                            solicitud.nombre(),
                            solicitud.correo()
                    );

            return Map.of(
                    "mensaje",
                    "Administrador registrado. Se ha enviado un enlace de activación a su correo.",
                    "id", administrador.getId(),
                    "usuario", administrador.getUsuario(),
                    "nombre", administrador.getNombre(),
                    "correo", administrador.getCorreo(),
                    "rol", administrador.getRol()
            );

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // =====================================================
    // DATOS RECIBIDOS
    // =====================================================

    public record CrearAdministradorRequest(
            String usuario,
            String nombre,
            String correo
    ) {
    }
}

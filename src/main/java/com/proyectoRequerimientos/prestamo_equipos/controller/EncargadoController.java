
package com.proyectoRequerimientos.prestamo_equipos.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    // =====================================================
    // LISTAR ENCARGADOS Y ADMINISTRADORES
    // =====================================================

    @GetMapping
    public List<Encargado> listarEncargados() {
        return encargadoService.listarEncargados();
    }

    // =====================================================
    // REGISTRAR ENCARGADO
    // =====================================================

    @PostMapping
    public Map<String, Object> crearEncargado(
            @RequestBody CrearEncargadoRequest solicitud) {

        if (solicitud == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes enviar los datos del encargado.");
        }

        try {

            Encargado encargado = encargadoService.crearEncargado(
                    solicitud.usuario(),
                    solicitud.nombre(),
                    solicitud.correo()
            );

            return Map.of(
                    "mensaje",
                    "Encargado registrado. Se ha enviado un enlace de activación a su correo.",
                    "id", encargado.getId(),
                    "nombre", encargado.getNombre(),
                    "usuario", encargado.getUsuario(),
                    "correo", encargado.getCorreo(),
                    "correoVerificado",
                    Boolean.TRUE.equals(encargado.getCorreoVerificado()),
                    "rol", encargado.getRol()
            );

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // =====================================================
    // REGISTRAR OTRO ADMINISTRADOR
    // =====================================================

    @PostMapping("/administradores")
    public Map<String, Object> crearAdministrador(
            @RequestBody CrearEncargadoRequest solicitud,
            Principal principal) {

        if (solicitud == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes enviar los datos del administrador.");
        }

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión.");
        }

        try {

            Encargado administrador = encargadoService.crearAdministrador(
                    solicitud.usuario(),
                    solicitud.nombre(),
                    solicitud.correo(),
                    principal.getName()
            );

            return Map.of(
                    "mensaje",
                    "Administrador registrado. Se ha enviado un enlace de activación a su correo.",
                    "id", administrador.getId(),
                    "nombre", administrador.getNombre(),
                    "usuario", administrador.getUsuario(),
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
    // EDITAR CUENTA ADMINISTRATIVA
    // =====================================================

    @PutMapping("/{id}")
    public Map<String, Object> editarCuenta(
            @PathVariable Long id,
            @RequestBody CrearEncargadoRequest solicitud,
            Principal principal) {

        if (solicitud == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes enviar los datos de la cuenta.");
        }

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión.");
        }

        try {

            Encargado encargado = encargadoService.editarCuenta(
                    id,
                    solicitud.usuario(),
                    solicitud.nombre(),
                    solicitud.correo(),
                    principal.getName()
            );

            return Map.of(
                    "mensaje",
                    Boolean.TRUE.equals(encargado.getCorreoVerificado())
                            ? "La cuenta se actualizó correctamente."
                            : "La cuenta se actualizó. Se debe verificar el correo electrónico.",
                    "id", encargado.getId(),
                    "nombre", encargado.getNombre(),
                    "usuario", encargado.getUsuario(),
                    "correo", encargado.getCorreo(),
                    "rol", encargado.getRol(),
                    "activo", encargado.isActivo(),
                    "correoVerificado",
                    Boolean.TRUE.equals(encargado.getCorreoVerificado())
            );

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // =====================================================
    // DAR DE BAJA UNA CUENTA
    // =====================================================

    @PatchMapping("/{id}/desactivar")
    public Map<String, Object> desactivarCuenta(
            @PathVariable Long id,
            Principal principal) {

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión.");
        }

        try {

            encargadoService.desactivarCuenta(
                    id,
                    principal.getName()
            );

            return Map.of(
                    "mensaje",
                    "La cuenta se desactivó correctamente.",
                    "id",
                    id
            );

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    // =====================================================
    // REACTIVAR UNA CUENTA
    // =====================================================

    @PatchMapping("/{id}/reactivar")
    public Map<String, Object> reactivarCuenta(
            @PathVariable Long id,
            Principal principal) {

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión.");
        }

        try {

            Encargado encargado = encargadoService.reactivarCuenta(
                    id,
                    principal.getName()
            );

            return Map.of(
                    "mensaje",
                    Boolean.TRUE.equals(encargado.getCorreoVerificado())
                            ? "La cuenta se reactivó correctamente."
                            : "La cuenta se reactivó. Se ha enviado un nuevo enlace de activación.",
                    "id", encargado.getId(),
                    "activo", encargado.isActivo(),
                    "correoVerificado",
                    Boolean.TRUE.equals(encargado.getCorreoVerificado())
            );

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }
}

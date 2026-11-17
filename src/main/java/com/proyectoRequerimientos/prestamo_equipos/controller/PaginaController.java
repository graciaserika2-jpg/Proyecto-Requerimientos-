
package com.proyectoRequerimientos.prestamo_equipos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaController {

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @GetMapping("/inicio")
    public String mostrarInicio() {
        return "inicio";
    }

    @GetMapping("/inventario")
    public String mostrarInventario() {
        return "inventario";
    }

    @GetMapping("/personas")
    public String mostrarPersonas() {
        return "personas";
    }

    @GetMapping("/prestamos")
    public String mostrarPrestamos() {
        return "prestamos";
    }

    @GetMapping("/bloqueos")
    public String mostrarBloqueos() {
        return "bloqueos";
    }

    @GetMapping("/encargados")
    public String mostrarEncargados() {
        return "encargados";
    }

    @GetMapping("/cambiar-password")
    public String mostrarCambiarPassword() {
        return "cambiar-password";
    }

    @GetMapping("/disponibilidad")
    public String mostrarDisponibilidad() {
        return "disponibilidad";
    }

    @GetMapping("/mi-historial")
    public String mostrarMiHistorial() {
        return "mi-historial";
    }

    @GetMapping("/activar-cuenta")
    public String mostrarActivarCuenta() {
        return "activar-cuenta";
    }

    @GetMapping("/olvide-password")
    public String mostrarOlvidePassword() {
        return "olvide-password";
    }

    @GetMapping("/restablecer-password")
    public String mostrarRestablecerPassword() {
        return "restablecer-password";
    }

    // Configuración inicial del primer administrador.
    @GetMapping("/configuracion-inicial")
    public String mostrarConfiguracionInicial() {
        return "configuracion-inicial";
    }
}

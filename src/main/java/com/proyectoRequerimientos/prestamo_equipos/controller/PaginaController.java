
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

    // Activación de cuentas.
    @GetMapping("/activar-cuenta")
    public String mostrarActivarCuenta() {
        return "activar-cuenta";
    }

    // Solicitud de recuperación de contraseña.
    @GetMapping("/olvide-password")
    public String mostrarOlvidePassword() {
        return "olvide-password";
    }

    // Formulario para establecer una contraseña nueva.
    @GetMapping("/restablecer-password")
    public String mostrarRestablecerPassword() {
        return "restablecer-password";
    }
}

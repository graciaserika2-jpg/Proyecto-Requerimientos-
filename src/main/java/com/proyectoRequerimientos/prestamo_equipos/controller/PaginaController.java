
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

}


package com.proyectoRequerimientos.prestamo_equipos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaController {

    @GetMapping("/inventario")
    public String mostrarInventario() {
        return "inventario";
    }
}

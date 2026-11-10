
package com.proyectoRequerimientos.prestamo_equipos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PrestamoEquiposApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrestamoEquiposApplication.class, args);
    }

}


package com.proyectoRequerimientos.prestamo_equipos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.proyectoRequerimientos.prestamo_equipos.model.Encargado;
import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;

@Configuration
public class EncargadoInicial {

    @Bean
    CommandLineRunner crearAdministradorInicial(
            EncargadoRepository repository,
            PasswordEncoder encoder) {

        return args -> {
            String usuario = System.getenv("ADMIN_USER");
            String password = System.getenv("ADMIN_PASSWORD");
            String nombre = System.getenv("ADMIN_NAME");

            if (usuario == null || usuario.isBlank()
                    || password == null || password.isBlank()
                    || nombre == null || nombre.isBlank()) {
                System.out.println(
                    "No se creó el administrador inicial: " +
                    "faltan variables de entorno."
                );
                return;
            }

            if (repository.count() == 0) {
                Encargado administrador = new Encargado();
                administrador.setUsuario(usuario.trim());
                administrador.setNombre(nombre.trim());
                administrador.setPasswordHash(
                    encoder.encode(password)
                );
                administrador.setRol("ADMIN");
                administrador.setActivo(true);

                repository.save(administrador);

                System.out.println(
                    "Cuenta inicial de administrador creada."
                );
            }
        };
    }
}

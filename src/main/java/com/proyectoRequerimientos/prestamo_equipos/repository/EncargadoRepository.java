
package com.proyectoRequerimientos.prestamo_equipos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectoRequerimientos.prestamo_equipos.model.Encargado;

public interface EncargadoRepository
        extends JpaRepository<Encargado, Long> {

    Optional<Encargado> findByUsuarioIgnoreCase(String usuario);

    boolean existsByCorreoIgnoreCase(String correo);

    Optional<Encargado> findByCorreoIgnoreCase(String correo);

    // Comprobar si ya existe una cuenta con determinado rol.
    boolean existsByRolIgnoreCase(String rol);
}

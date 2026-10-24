
package com.proyectoRequerimientos.prestamo_equipos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectoRequerimientos.prestamo_equipos.model.CuentaPersona;

public interface CuentaPersonaRepository
        extends JpaRepository<CuentaPersona, Long> {

    Optional<CuentaPersona> findByUsuarioIgnoreCase(String usuario);

    Optional<CuentaPersona> findByPersona_Id(Long personaId);

    boolean existsByUsuarioIgnoreCase(String usuario);
}

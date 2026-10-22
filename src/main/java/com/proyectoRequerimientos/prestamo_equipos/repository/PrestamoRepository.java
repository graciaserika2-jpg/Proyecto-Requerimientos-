
package com.proyectoRequerimientos.prestamo_equipos.repository;

import com.proyectoRequerimientos.prestamo_equipos.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrestamoRepository
        extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findAllByOrderByFechaPrestamoDesc();

    boolean existsByEquipo_IdAndEstado(Long equipoId, String estado);

    boolean existsByPersona_IdAndEstado(Long personaId, String estado);
}

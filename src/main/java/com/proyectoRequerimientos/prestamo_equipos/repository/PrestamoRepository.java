
package com.proyectoRequerimientos.prestamo_equipos.repository;

import com.proyectoRequerimientos.prestamo_equipos.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findAllByOrderByFechaPrestamoDesc();

    List<Prestamo> findByPersona_IdOrderByFechaPrestamoDesc(
            Long personaId);

    boolean existsByEquipo_IdAndEstado(
            Long equipoId, String estado);

    boolean existsByEquipo_IdAndEstadoIn(
            Long equipoId, List<String> estados);

    boolean existsByPersona_IdAndEstado(
            Long personaId, String estado);

    boolean existsByPersona_IdAndEstadoIn(
            Long personaId, List<String> estados);

    // Préstamos cuya fecha prevista es anterior a la fecha indicada.
    List<Prestamo> findByEstadoAndFechaDevolucionPrevistaBefore(
            String estado, LocalDate fecha);

    // Préstamos cuya fecha prevista es igual o anterior a la fecha indicada.
    List<Prestamo> findByEstadoAndFechaDevolucionPrevistaLessThanEqual(
            String estado, LocalDate fecha);
}

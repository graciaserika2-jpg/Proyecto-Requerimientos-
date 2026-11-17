
package com.proyectoRequerimientos.prestamo_equipos.repository;

import com.proyectoRequerimientos.prestamo_equipos.model.BloqueoAdministrativo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BloqueoAdministrativoRepository
        extends JpaRepository<BloqueoAdministrativo, Long> {

    boolean existsByPersona_IdAndActivoTrue(Long personaId);

    boolean existsByPrestamo_Id(Long prestamoId);

    Optional<BloqueoAdministrativo>
            findFirstByPersona_IdAndActivoTrueOrderByFechaBloqueoDesc(
                    Long personaId);

    List<BloqueoAdministrativo>
            findByPersona_IdOrderByFechaBloqueoDesc(Long personaId);

    List<BloqueoAdministrativo>
            findByActivoTrueOrderByFechaBloqueoDesc();
}

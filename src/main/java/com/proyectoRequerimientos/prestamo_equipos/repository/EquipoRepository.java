
package com.proyectoRequerimientos.prestamo_equipos.repository;

import com.proyectoRequerimientos.prestamo_equipos.model.Equipo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    List<Equipo> findByActivoTrueOrActivoIsNull();

    @Query("""
        SELECT e FROM Equipo e
        WHERE (e.activo = true OR e.activo IS NULL)
          AND LOWER(e.estado) = 'disponible'
          AND (
            LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(COALESCE(e.tipo, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(COALESCE(e.descripcion, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(COALESCE(e.numeroSerie, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
          )
        ORDER BY e.nombre
        """)
    List<Equipo> buscarDisponiblesParaPrestamo(
            @Param("texto") String texto,
            Pageable pageable
    );
}

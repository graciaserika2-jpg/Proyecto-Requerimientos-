
package com.proyectoRequerimientos.prestamo_equipos.repository;

import com.proyectoRequerimientos.prestamo_equipos.model.Persona;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    List<Persona> findByActivoTrueOrActivoIsNull();

    boolean existsByIdentificadorIgnoreCase(String identificador);

    @Query("""
        SELECT p FROM Persona p
        WHERE (p.activo = true OR p.activo IS NULL)
          AND (
            LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(p.identificador) LIKE LOWER(CONCAT('%', :texto, '%'))
          )
        ORDER BY p.nombre
        """)
    List<Persona> buscarParaPrestamo(
            @Param("texto") String texto,
            Pageable pageable
    );
}

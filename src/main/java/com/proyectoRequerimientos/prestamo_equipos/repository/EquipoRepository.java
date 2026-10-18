package com.proyectoRequerimientos.prestamo_equipos.repository;


import com.proyectoRequerimientos.prestamo_equipos.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

}

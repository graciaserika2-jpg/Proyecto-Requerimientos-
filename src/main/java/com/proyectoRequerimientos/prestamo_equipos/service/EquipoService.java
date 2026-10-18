package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.model.Equipo;
import com.proyectoRequerimientos.prestamo_equipos.repository.EquipoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;

    public EquipoService(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    public List<Equipo> listarEquipos() {
        return equipoRepository.findAll();
    }

    public Equipo guardarEquipo(Equipo equipo) {
        return equipoRepository.save(equipo);
    }

    public Equipo actualizarEquipo(Long id, Equipo equipo) {

        Equipo existente = equipoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));

        existente.setNombre(equipo.getNombre());
        existente.setTipo(equipo.getTipo());
        existente.setDescripcion(equipo.getDescripcion());
        existente.setNumeroSerie(equipo.getNumeroSerie());
        existente.setEstado(equipo.getEstado());

        return equipoRepository.save(existente);
    }

    public void eliminarEquipo(Long id) {
        Equipo existente = equipoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));

        equipoRepository.delete(existente);
    }

}

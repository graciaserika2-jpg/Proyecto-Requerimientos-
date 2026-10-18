
package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.model.Equipo;
import com.proyectoRequerimientos.prestamo_equipos.repository.EquipoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.Locale;

@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "Cómputo",
            "Proyección",
            "Audio",
            "Cableado",
            "Accesorios"
    );

    private static final Set<String> ESTADOS_PERMITIDOS = Set.of(
            "Disponible",
            "Prestado",
            "Mantenimiento"
    );

    public EquipoService(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    public List<Equipo> listarEquipos() {
        return equipoRepository.findAll();
    }

    public Equipo guardarEquipo(Equipo equipo) {
        validarEquipo(equipo);

        if ("Prestado".equals(equipo.getEstado())) {
            throw new IllegalArgumentException(
                    "Un artículo nuevo no puede registrarse como Prestado"
            );
        }

        return equipoRepository.save(equipo);
    }

    public Equipo actualizarEquipo(Long id, Equipo equipo) {
        Equipo existente = equipoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Artículo no encontrado"));

        validarEquipo(equipo);

        existente.setNombre(equipo.getNombre());
        existente.setTipo(equipo.getTipo());
        existente.setDescripcion(equipo.getDescripcion());
        existente.setNumeroSerie(equipo.getNumeroSerie());
        existente.setEstado(equipo.getEstado());

        return equipoRepository.save(existente);
    }

    private void validarEquipo(Equipo equipo) {
        if (equipo == null) {
            throw new IllegalArgumentException(
                    "Los datos del artículo son obligatorios"
            );
        }

        if (equipo.getNombre() == null
                || equipo.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del artículo es obligatorio"
            );
        }

        if (equipo.getTipo() == null
                || equipo.getTipo().isBlank()) {
            throw new IllegalArgumentException(
                    "El tipo de artículo es obligatorio"
            );
        }

        equipo.setNombre(equipo.getNombre().trim());
        equipo.setTipo(equipo.getTipo().trim());

        if (!TIPOS_PERMITIDOS.contains(equipo.getTipo())) {
            throw new IllegalArgumentException(
                    "El tipo de artículo no está permitido"
            );
        }

        if (equipo.getEstado() == null
                || equipo.getEstado().isBlank()) {
            throw new IllegalArgumentException(
                    "El estado del artículo es obligatorio"
            );
        }

        equipo.setEstado(equipo.getEstado().trim());

        if (!ESTADOS_PERMITIDOS.contains(equipo.getEstado())) {
            throw new IllegalArgumentException(
                    "El estado del artículo no es válido"
            );
        }

        if (equipo.getDescripcion() != null) {
            equipo.setDescripcion(equipo.getDescripcion().trim());
        }

        if (equipo.getNumeroSerie() != null) {
            equipo.setNumeroSerie(equipo.getNumeroSerie().trim());
        }

        String nombreNormalizado = equipo.getNombre()
                .toLowerCase(Locale.ROOT);

        boolean requiereSerie =
                nombreNormalizado.contains("laptop")
                || nombreNormalizado.contains("proyector");

        if (requiereSerie
                && (equipo.getNumeroSerie() == null
                || equipo.getNumeroSerie().isBlank())) {
            throw new IllegalArgumentException(
                    "Este artículo requiere un número de serie"
            );
        }
    }

    public void eliminarEquipo(Long id) {
        Equipo existente = equipoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Artículo no encontrado"));

        equipoRepository.delete(existente);
    }
}

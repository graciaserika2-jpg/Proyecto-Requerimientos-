
package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.dto.EquipoDisponibilidad;
import com.proyectoRequerimientos.prestamo_equipos.model.Equipo;
import com.proyectoRequerimientos.prestamo_equipos.repository.EquipoRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    public List<Equipo> buscarDisponiblesParaPrestamo(String texto) {
        if (texto == null || texto.trim().length() < 2) {
            return List.of();
        }

        return equipoRepository.buscarDisponiblesParaPrestamo(
                texto.trim(),
                PageRequest.of(0, 10)
        );
    }

    public List<Equipo> listarEquipos() {
        return equipoRepository.findByActivoTrueOrActivoIsNull();
    }

    public Equipo guardarEquipo(Equipo equipo) {
        validarEquipo(equipo);

        if ("Prestado".equals(equipo.getEstado())) {
            throw new IllegalArgumentException(
                    "Un artículo nuevo no puede registrarse como Prestado"
            );
        }

        equipo.setActivo(true);

        return equipoRepository.save(equipo);
    }

    public Equipo actualizarEquipo(Long id, Equipo equipo) {
        Equipo existente = buscarActivo(id);

        validarEquipo(equipo);

        // Si el artículo ya tiene un tipo asignado,
        // no permitimos cambiarlo.
        if (existente.getTipo() != null
                && !existente.getTipo().isBlank()
                && !existente.getTipo().equals(equipo.getTipo())) {

            throw new IllegalArgumentException(
                    "No se puede cambiar el tipo de un artículo existente."
            );
        }

        // El inventario no puede asignar manualmente
        // el estado Prestado.
        if ("Prestado".equals(equipo.getEstado())) {
            throw new IllegalArgumentException(
                    "El estado Prestado debe gestionarse desde Préstamos."
            );
        }

        // No permitimos editar artículos prestados.
        if ("Prestado".equals(existente.getEstado())) {
            throw new IllegalArgumentException(
                    "No se puede editar un artículo prestado desde Inventario."
            );
        }

        // Actualizamos los datos del artículo.
        existente.setNombre(equipo.getNombre());
        existente.setDescripcion(equipo.getDescripcion());
        existente.setNumeroSerie(equipo.getNumeroSerie());
        existente.setEstado(equipo.getEstado());

        // Si el artículo no tenía tipo, se permite asignarlo
        // por primera vez.
        if (existente.getTipo() == null
                || existente.getTipo().isBlank()) {
            existente.setTipo(equipo.getTipo());
        }

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
        Equipo existente = buscarActivo(id);

        // Solamente se pueden eliminar artículos disponibles.
        if (!"Disponible".equals(existente.getEstado())) {
            throw new IllegalArgumentException(
                    "No se puede eliminar un artículo que está "
                    + "prestado o en mantenimiento. "
                    + "Solamente se pueden eliminar artículos disponibles."
            );
        }

        // Eliminación lógica: el registro permanece en la base de datos.
        existente.setActivo(false);

        equipoRepository.save(existente);
    }

    private Equipo buscarActivo(Long id) {
        return equipoRepository.findById(id)
                .filter(equipo ->
                        !Boolean.FALSE.equals(equipo.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Artículo no encontrado"
                ));
    }

    public List<EquipoDisponibilidad> listarDisponibilidad() {
        return listarEquipos().stream()
                .map(equipo -> new EquipoDisponibilidad(
                        equipo.getNombre(),
                        equipo.getTipo(),
                        equipo.getDescripcion(),
                        equipo.getEstado()
                ))
                .toList();
    }
}

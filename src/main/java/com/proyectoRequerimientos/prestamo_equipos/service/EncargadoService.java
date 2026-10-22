
package com.proyectoRequerimientos.prestamo_equipos.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyectoRequerimientos.prestamo_equipos.model.Encargado;
import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;

@Service
public class EncargadoService {

    private final EncargadoRepository encargadoRepository;
    private final PasswordEncoder passwordEncoder;

    public EncargadoService(
            EncargadoRepository encargadoRepository,
            PasswordEncoder passwordEncoder) {
        this.encargadoRepository = encargadoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Encargado> listarEncargados() {
        return encargadoRepository.findAll()
                .stream()
                .filter(Encargado::isActivo)
                .toList();
    }

    @Transactional
    public Encargado crearEncargado(
            String usuario,
            String nombre,
            String password) {

        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de usuario es obligatorio.");
        }

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre completo es obligatorio.");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres.");
        }

        String usuarioLimpio = usuario.trim();

        if (encargadoRepository
                .findByUsuarioIgnoreCase(usuarioLimpio)
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Ese nombre de usuario ya está registrado.");
        }

        Encargado encargado = new Encargado();
        encargado.setUsuario(usuarioLimpio);
        encargado.setNombre(nombre.trim());
        encargado.setPasswordHash(passwordEncoder.encode(password));
        encargado.setRol("ENCARGADO");
        encargado.setActivo(true);

        return encargadoRepository.save(encargado);
    }
}

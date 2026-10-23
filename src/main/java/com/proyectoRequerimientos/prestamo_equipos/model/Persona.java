package com.proyectoRequerimientos.prestamo_equipos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "personas")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String identificador;

    @Column(nullable = false)
    private Boolean activo = true;

    public Persona() {
    }

    @PrePersist
    @PreUpdate
    private void validarDatos() {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la persona es obligatorio.");
        }

        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException(
                    "El tipo de persona es obligatorio.");
        }

        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador de la persona es obligatorio.");
        }

        String tipoNormalizado = tipo.trim();

        if (tipoNormalizado.equalsIgnoreCase("Estudiante")) {
            if (!identificador.matches("[0-9]{10}")) {
                throw new IllegalArgumentException(
                        "La boleta del estudiante debe contener exactamente 10 dígitos, sin letras, espacios ni símbolos.");
            }
        } else if (tipoNormalizado.equalsIgnoreCase("Profesor")) {
            if (identificador.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "El número de empleado del profesor es obligatorio.");
            }
        } else {
            throw new IllegalArgumentException(
                    "El tipo de persona debe ser Estudiante o Profesor.");
        }

        nombre = nombre.trim();
        tipo = tipoNormalizado;
        identificador = identificador.trim();

        if (activo == null) {
            activo = true;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}

package com.proyectoRequerimientos.prestamo_equipos.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "personas")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String tipo = "Alumno";

    private String identificador;

    private Boolean activo = true;

    private LocalDate fechaFinSuspension;

    public Persona() {
    }

    @PrePersist
    public void validarDatos() {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del alumno es obligatorio."
            );
        }

        if (tipo == null || !tipo.equalsIgnoreCase("Alumno")) {
            throw new IllegalArgumentException(
                    "El tipo de persona debe ser Alumno."
            );
        }

        if (identificador == null
                || !identificador.matches("[0-9]{10}")) {
            throw new IllegalArgumentException(
                    "La boleta debe contener exactamente 10 dígitos."
            );
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

    public LocalDate getFechaFinSuspension() {
        return fechaFinSuspension;
    }

    public void setFechaFinSuspension(LocalDate fechaFinSuspension) {
        this.fechaFinSuspension = fechaFinSuspension;
    }
}

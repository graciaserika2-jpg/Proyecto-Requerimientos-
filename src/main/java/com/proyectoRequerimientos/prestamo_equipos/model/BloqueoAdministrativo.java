
package com.proyectoRequerimientos.prestamo_equipos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bloqueos_administrativos")
public class BloqueoAdministrativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @OneToOne(optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false, unique = true)
    private Prestamo prestamo;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fechaBloqueo;

    @Column(nullable = false, length = 80)
    private String bloqueadoPor;

    @Column(nullable = false)
    private boolean activo = true;

    private LocalDateTime fechaLevantamiento;

    @Column(length = 80)
    private String levantadoPor;

    @Column(length = 1000)
    private String motivoLevantamiento;

    public BloqueoAdministrativo() {
    }

    public Long getId() {
        return id;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getFechaBloqueo() {
        return fechaBloqueo;
    }

    public void setFechaBloqueo(LocalDateTime fechaBloqueo) {
        this.fechaBloqueo = fechaBloqueo;
    }

    public String getBloqueadoPor() {
        return bloqueadoPor;
    }

    public void setBloqueadoPor(String bloqueadoPor) {
        this.bloqueadoPor = bloqueadoPor;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaLevantamiento() {
        return fechaLevantamiento;
    }

    public void setFechaLevantamiento(LocalDateTime fechaLevantamiento) {
        this.fechaLevantamiento = fechaLevantamiento;
    }

    public String getLevantadoPor() {
        return levantadoPor;
    }

    public void setLevantadoPor(String levantadoPor) {
        this.levantadoPor = levantadoPor;
    }

    public String getMotivoLevantamiento() {
        return motivoLevantamiento;
    }

    public void setMotivoLevantamiento(String motivoLevantamiento) {
        this.motivoLevantamiento = motivoLevantamiento;
    }
}

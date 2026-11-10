
package com.proyectoRequerimientos.prestamo_equipos.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipo_id")
    private Equipo equipo;

    private LocalDateTime fechaPrestamo;

    private LocalDate fechaDevolucionPrevista;

    private LocalDateTime fechaDevolucionReal;

    private String estado;

    private String observaciones;

    // Información de la devolución
    private String condicionDevolucion;

    @Column(length = 1000)
    private String detallesDanio;

    private String recibidoPor;

    // Información de la revisión de mantenimiento
    @Column(length = 1000)
    private String revisionMantenimiento;

    private LocalDateTime fechaRevision;

    private String revisadoPor;

    private Boolean mantenimientoAtendido = false;

    public Prestamo() {
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

    public Equipo getEquipo() {
        return equipo;
    }

    public void setEquipo(Equipo equipo) {
        this.equipo = equipo;
    }

    public LocalDateTime getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDateTime fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public LocalDate getFechaDevolucionPrevista() {
        return fechaDevolucionPrevista;
    }

    public void setFechaDevolucionPrevista(
            LocalDate fechaDevolucionPrevista) {
        this.fechaDevolucionPrevista = fechaDevolucionPrevista;
    }

    public LocalDateTime getFechaDevolucionReal() {
        return fechaDevolucionReal;
    }

    public void setFechaDevolucionReal(
            LocalDateTime fechaDevolucionReal) {
        this.fechaDevolucionReal = fechaDevolucionReal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getCondicionDevolucion() {
        return condicionDevolucion;
    }

    public void setCondicionDevolucion(String condicionDevolucion) {
        this.condicionDevolucion = condicionDevolucion;
    }

    public String getDetallesDanio() {
        return detallesDanio;
    }

    public void setDetallesDanio(String detallesDanio) {
        this.detallesDanio = detallesDanio;
    }

    public String getRecibidoPor() {
        return recibidoPor;
    }

    public void setRecibidoPor(String recibidoPor) {
        this.recibidoPor = recibidoPor;
    }

    public String getRevisionMantenimiento() {
        return revisionMantenimiento;
    }

    public void setRevisionMantenimiento(String revisionMantenimiento) {
        this.revisionMantenimiento = revisionMantenimiento;
    }

    public LocalDateTime getFechaRevision() {
        return fechaRevision;
    }

    public void setFechaRevision(LocalDateTime fechaRevision) {
        this.fechaRevision = fechaRevision;
    }

    public String getRevisadoPor() {
        return revisadoPor;
    }

    public void setRevisadoPor(String revisadoPor) {
        this.revisadoPor = revisadoPor;
    }

    public Boolean getMantenimientoAtendido() {
        return mantenimientoAtendido;
    }

    public void setMantenimientoAtendido(Boolean mantenimientoAtendido) {
        this.mantenimientoAtendido = mantenimientoAtendido;
    }
}

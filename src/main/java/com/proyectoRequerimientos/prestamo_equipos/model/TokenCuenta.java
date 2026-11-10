package com.proyectoRequerimientos.prestamo_equipos.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(
    name = "tokens_cuenta",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_token_cuenta_hash",
            columnNames = "token_hash"
        )
    }
)
public class TokenCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cuenta de alumno o profesor.
    // Puede ser null si el token pertenece a un encargado.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_persona_id")
    private CuentaPersona cuentaPersona;

    // Cuenta de encargado.
    // Puede ser null si el token pertenece a una persona.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encargado_id")
    private Encargado encargado;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    // ACTIVACION o RECUPERACION.
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public TokenCuenta() {
    }

    @PrePersist
    protected void antesDeGuardar() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CuentaPersona getCuentaPersona() {
        return cuentaPersona;
    }

    public void setCuentaPersona(CuentaPersona cuentaPersona) {
        this.cuentaPersona = cuentaPersona;
    }

    public Encargado getEncargado() {
        return encargado;
    }

    public void setEncargado(Encargado encargado) {
        this.encargado = encargado;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public boolean isUsado() {
        return usado;
    }

    public void setUsado(boolean usado) {
        this.usado = usado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
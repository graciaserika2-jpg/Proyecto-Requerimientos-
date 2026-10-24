
package com.proyectoRequerimientos.prestamo_equipos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(
    name = "cuentas_persona",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cuenta_persona_usuario",
            columnNames = "usuario"
        ),
        @UniqueConstraint(
            name = "uk_cuenta_persona_persona",
            columnNames = "persona_id"
        )
    }
)
public class CuentaPersona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String usuario;

    @JsonIgnore
    @Column(nullable = false)
    private String passwordHash;

    @OneToOne(optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private boolean debeCambiarPassword = true;

    public CuentaPersona() {
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public void setDebeCambiarPassword(boolean debeCambiarPassword) {
        this.debeCambiarPassword = debeCambiarPassword;
    }
}

package com.proyectoRequerimientos.prestamo_equipos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import com.proyectoRequerimientos.prestamo_equipos.model.TokenCuenta;

public interface TokenCuentaRepository
        extends JpaRepository<TokenCuenta, Long> {

    Optional<TokenCuenta> findByTokenHash(String tokenHash);

    // Buscar un token aplicando bloqueo de escritura.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenCuenta> findWithLockByTokenHash(String tokenHash);

    List<TokenCuenta> findAllByCuentaPersona_IdAndTipoAndUsadoFalse(
            Long cuentaPersonaId,
            String tipo
    );

    List<TokenCuenta> findAllByEncargado_IdAndTipoAndUsadoFalse(
            Long encargadoId,
            String tipo
    );
}
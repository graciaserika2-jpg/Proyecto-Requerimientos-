
package com.proyectoRequerimientos.prestamo_equipos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.proyectoRequerimientos.prestamo_equipos.model.TokenCuenta;

public interface TokenCuentaRepository
        extends JpaRepository<TokenCuenta, Long> {

    Optional<TokenCuenta> findByTokenHash(String tokenHash);

    // Buscar el token y bloquear su registro durante la transacción.
    // Se utiliza SQL nativo para evitar la sintaxis de bloqueo
    // que Hibernate genera y que presenta incompatibilidad con TiDB.
    @Query(
        value = "SELECT * FROM tokens_cuenta " +
                "WHERE token_hash = :tokenHash FOR UPDATE",
        nativeQuery = true
    )
    Optional<TokenCuenta> findWithLockByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    List<TokenCuenta> findAllByCuentaPersona_IdAndTipoAndUsadoFalse(
            Long cuentaPersonaId,
            String tipo
    );

    List<TokenCuenta> findAllByEncargado_IdAndTipoAndUsadoFalse(
            Long encargadoId,
            String tipo
    );
}

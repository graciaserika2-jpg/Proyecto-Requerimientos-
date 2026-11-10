package com.proyectoRequerimientos.prestamo_equipos.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyectoRequerimientos.prestamo_equipos.model.CuentaPersona;
import com.proyectoRequerimientos.prestamo_equipos.model.Encargado;
import com.proyectoRequerimientos.prestamo_equipos.model.TokenCuenta;
import com.proyectoRequerimientos.prestamo_equipos.repository.TokenCuentaRepository;

@Service
public class TokenCuentaService {

    private final TokenCuentaRepository tokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public static final String ACTIVACION = "ACTIVACION";
    public static final String RECUPERACION = "RECUPERACION";

    private static final int HORAS_ACTIVACION = 24;
    private static final int MINUTOS_RECUPERACION = 30;

    public TokenCuentaService(
            TokenCuentaRepository tokenRepository) {

        this.tokenRepository = tokenRepository;
    }

    // =====================================================
    // ACTIVACIÓN DE CUENTAS
    // =====================================================

    @Transactional
    public String crearTokenActivacion(CuentaPersona cuenta) {

        if (cuenta == null || cuenta.getId() == null) {
            throw new IllegalArgumentException(
                    "La cuenta de la persona no es válida.");
        }

        invalidarTokensPersona(cuenta.getId(), ACTIVACION);

        return generarYGuardarToken(
                cuenta, null, ACTIVACION, HORAS_ACTIVACION, true);
    }

    @Transactional
    public String crearTokenActivacion(Encargado encargado) {

        if (encargado == null || encargado.getId() == null) {
            throw new IllegalArgumentException(
                    "La cuenta del encargado no es válida.");
        }

        invalidarTokensEncargado(encargado.getId(), ACTIVACION);

        return generarYGuardarToken(
                null, encargado, ACTIVACION, HORAS_ACTIVACION, true);
    }

    // =====================================================
    // RECUPERACIÓN DE CONTRASEÑA
    // =====================================================

    @Transactional
    public String crearTokenRecuperacion(CuentaPersona cuenta) {

        if (cuenta == null || cuenta.getId() == null) {
            throw new IllegalArgumentException(
                    "La cuenta de la persona no es válida.");
        }

        invalidarTokensPersona(cuenta.getId(), RECUPERACION);

        return generarYGuardarToken(
                cuenta, null, RECUPERACION, MINUTOS_RECUPERACION, false);
    }

    @Transactional
    public String crearTokenRecuperacion(Encargado encargado) {

        if (encargado == null || encargado.getId() == null) {
            throw new IllegalArgumentException(
                    "La cuenta del encargado no es válida.");
        }

        invalidarTokensEncargado(encargado.getId(), RECUPERACION);

        return generarYGuardarToken(
                null, encargado, RECUPERACION, MINUTOS_RECUPERACION, false);
    }

    // =====================================================
    // GENERACIÓN Y ALMACENAMIENTO
    // =====================================================

    private String generarYGuardarToken(
            CuentaPersona cuenta,
            Encargado encargado,
            String tipo,
            int duracion,
            boolean horas) {

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String tokenOriginal = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        TokenCuenta token = new TokenCuenta();

        token.setCuentaPersona(cuenta);
        token.setEncargado(encargado);
        token.setTokenHash(calcularHash(tokenOriginal));
        token.setTipo(tipo);

        LocalDateTime expiracion = LocalDateTime.now();

        if (horas) {
            expiracion = expiracion.plusHours(duracion);
        } else {
            expiracion = expiracion.plusMinutes(duracion);
        }

        token.setFechaExpiracion(expiracion);
        token.setUsado(false);

        tokenRepository.save(token);

        return tokenOriginal;
    }

    // =====================================================
    // VALIDACIÓN Y CONSUMO DEL TOKEN
    // =====================================================

    @Transactional
    public TokenCuenta validarYConsumirToken(
            String tokenOriginal,
            String tipoEsperado) {

        if (tokenOriginal == null || tokenOriginal.isBlank()) {
            throw new IllegalArgumentException(
                    "El enlace no es válido.");
        }

        if (!ACTIVACION.equals(tipoEsperado)
                && !RECUPERACION.equals(tipoEsperado)) {

            throw new IllegalArgumentException(
                    "El tipo de operación no es válido.");
        }

        String hash = calcularHash(tokenOriginal);

        TokenCuenta token = tokenRepository
                .findWithLockByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El enlace no existe o no es válido."));

        if (!tipoEsperado.equals(token.getTipo())) {
            throw new IllegalArgumentException(
                    "El enlace no corresponde a esta operación.");
        }

        if (token.isUsado()) {
            throw new IllegalArgumentException(
                    "Este enlace ya fue utilizado.");
        }

        if (!token.getFechaExpiracion().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "El enlace ha expirado. Solicita uno nuevo.");
        }

        token.setUsado(true);

        tokenRepository.save(token);

        return token;
    }

    // =====================================================
    // INVALIDAR TOKENS ANTERIORES
    // =====================================================

    private void invalidarTokensPersona(
            Long cuentaPersonaId,
            String tipo) {

        List<TokenCuenta> tokens =
                tokenRepository
                        .findAllByCuentaPersona_IdAndTipoAndUsadoFalse(
                                cuentaPersonaId, tipo);

        tokens.forEach(token -> token.setUsado(true));

        tokenRepository.saveAll(tokens);
    }

    private void invalidarTokensEncargado(
            Long encargadoId,
            String tipo) {

        List<TokenCuenta> tokens =
                tokenRepository
                        .findAllByEncargado_IdAndTipoAndUsadoFalse(
                                encargadoId, tipo);

        tokens.forEach(token -> token.setUsado(true));

        tokenRepository.saveAll(tokens);
    }

    // =====================================================
    // HASH SHA-256
    // =====================================================

    private String calcularHash(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "No se pudo generar el hash del token.", e);
        }
    }
}
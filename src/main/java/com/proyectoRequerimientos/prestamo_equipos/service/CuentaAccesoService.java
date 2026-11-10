
package com.proyectoRequerimientos.prestamo_equipos.service;

import com.proyectoRequerimientos.prestamo_equipos.model.CuentaPersona;
import com.proyectoRequerimientos.prestamo_equipos.model.Encargado;
import com.proyectoRequerimientos.prestamo_equipos.model.TokenCuenta;
import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class CuentaAccesoService {

    private final CuentaPersonaRepository cuentaPersonaRepository;
    private final EncargadoRepository encargadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenCuentaService tokenCuentaService;
    private final CorreoService correoService;

    public CuentaAccesoService(
            CuentaPersonaRepository cuentaPersonaRepository,
            EncargadoRepository encargadoRepository,
            PasswordEncoder passwordEncoder,
            TokenCuentaService tokenCuentaService,
            CorreoService correoService) {

        this.cuentaPersonaRepository = cuentaPersonaRepository;
        this.encargadoRepository = encargadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenCuentaService = tokenCuentaService;
        this.correoService = correoService;
    }

    @Transactional
    public void activarCuenta(
            String token,
            String password,
            String confirmarPassword) {

        validarPassword(password, confirmarPassword);

        TokenCuenta tokenCuenta =
                tokenCuentaService.validarYConsumirToken(
                        token,
                        TokenCuentaService.ACTIVACION
                );

        if (tokenCuenta.getCuentaPersona() != null
                && tokenCuenta.getEncargado() == null) {

            CuentaPersona cuenta = tokenCuenta.getCuentaPersona();

            cuenta.setPasswordHash(
                    passwordEncoder.encode(password));

            cuenta.setCorreoVerificado(true);
            cuenta.setFechaActivacion(LocalDateTime.now());
            cuenta.setActivo(true);
            cuenta.setDebeCambiarPassword(false);

            cuentaPersonaRepository.save(cuenta);

        } else if (tokenCuenta.getEncargado() != null
                && tokenCuenta.getCuentaPersona() == null) {

            Encargado encargado = tokenCuenta.getEncargado();

            encargado.setPasswordHash(
                    passwordEncoder.encode(password));

            encargado.setCorreoVerificado(true);
            encargado.setFechaActivacion(LocalDateTime.now());
            encargado.setActivo(true);
            encargado.setDebeCambiarPassword(false);

            encargadoRepository.save(encargado);

        } else {

            throw new IllegalArgumentException(
                    "El enlace de activación no es válido.");
        }
    }

    @Transactional
    public void solicitarRecuperacion(String correo) {

        if (correo == null || correo.isBlank()) {
            return;
        }

        String correoNormalizado =
                correo.trim().toLowerCase(Locale.ROOT);

        CuentaPersona cuentaPersona =
                cuentaPersonaRepository
                        .findByCorreoIgnoreCase(correoNormalizado)
                        .orElse(null);

        if (cuentaPersona != null) {

            if (!cuentaPersona.isActivo()
                    || Boolean.FALSE.equals(
                            cuentaPersona.getCorreoVerificado())) {
                return;
            }

            String token =
                    tokenCuentaService.crearTokenRecuperacion(
                            cuentaPersona);

            correoService.enviarRecuperacion(
                    correoNormalizado,
                    cuentaPersona.getPersona().getNombre(),
                    token
            );

            return;
        }

        Encargado encargado =
                encargadoRepository
                        .findByCorreoIgnoreCase(correoNormalizado)
                        .orElse(null);

        if (encargado != null) {

            if (!encargado.isActivo()
                    || Boolean.FALSE.equals(
                            encargado.getCorreoVerificado())) {
                return;
            }

            String token =
                    tokenCuentaService.crearTokenRecuperacion(
                            encargado);

            correoService.enviarRecuperacion(
                    correoNormalizado,
                    encargado.getNombre(),
                    token
            );
        }
    }

    @Transactional
    public void restablecerPassword(
            String token,
            String password,
            String confirmarPassword) {

        validarPassword(password, confirmarPassword);

        TokenCuenta tokenCuenta =
                tokenCuentaService.validarYConsumirToken(
                        token,
                        TokenCuentaService.RECUPERACION
                );

        if (tokenCuenta.getCuentaPersona() != null
                && tokenCuenta.getEncargado() == null) {

            CuentaPersona cuenta = tokenCuenta.getCuentaPersona();

            if (!cuenta.isActivo()
                    || Boolean.FALSE.equals(
                            cuenta.getCorreoVerificado())) {

                throw new IllegalArgumentException(
                        "La cuenta no está disponible para recuperación.");
            }

            cuenta.setPasswordHash(
                    passwordEncoder.encode(password));

            cuenta.setDebeCambiarPassword(false);

            cuentaPersonaRepository.save(cuenta);

        } else if (tokenCuenta.getEncargado() != null
                && tokenCuenta.getCuentaPersona() == null) {

            Encargado encargado = tokenCuenta.getEncargado();

            if (!encargado.isActivo()
                    || Boolean.FALSE.equals(
                            encargado.getCorreoVerificado())) {

                throw new IllegalArgumentException(
                        "La cuenta no está disponible para recuperación.");
            }

            encargado.setPasswordHash(
                    passwordEncoder.encode(password));

            encargado.setDebeCambiarPassword(false);

            encargadoRepository.save(encargado);

        } else {

            throw new IllegalArgumentException(
                    "El enlace de recuperación no es válido.");
        }
    }

    private void validarPassword(
            String password,
            String confirmarPassword) {

        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 12 caracteres.");
        }

        if (!password.equals(confirmarPassword)) {
            throw new IllegalArgumentException(
                    "La confirmación de la contraseña no coincide.");
        }
    }
}

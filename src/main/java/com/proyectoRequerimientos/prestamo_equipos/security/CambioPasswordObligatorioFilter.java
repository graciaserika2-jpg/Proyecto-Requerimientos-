
package com.proyectoRequerimientos.prestamo_equipos.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CambioPasswordObligatorioFilter extends OncePerRequestFilter {

    private final CuentaPersonaRepository cuentaPersonaRepository;
    private final EncargadoRepository encargadoRepository;

    public CambioPasswordObligatorioFilter(
            CuentaPersonaRepository cuentaPersonaRepository,
            EncargadoRepository encargadoRepository) {

        this.cuentaPersonaRepository = cuentaPersonaRepository;
        this.encargadoRepository = encargadoRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion != null
                && autenticacion.isAuthenticated()
                && !"anonymousUser".equals(autenticacion.getPrincipal())) {

            String usuario = autenticacion.getName();

            boolean debeCambiarPassword = false;

            var encargado = encargadoRepository
                    .findByUsuarioIgnoreCase(usuario)
                    .filter(empleado -> empleado.isActivo());

            if (encargado.isPresent()) {

                debeCambiarPassword =
                        encargado.get().isDebeCambiarPassword();

            } else {

                var cuenta = cuentaPersonaRepository
                        .findByUsuarioIgnoreCase(usuario)
                        .filter(persona -> persona.isActivo());

                if (cuenta.isPresent()) {
                    debeCambiarPassword =
                            cuenta.get().isDebeCambiarPassword();
                }
            }

            if (debeCambiarPassword) {

                String ruta = request.getServletPath();

                boolean rutaPermitida =
                        ruta.equals("/cambiar-password")
                        || ruta.equals("/api/mi-cuenta/cambiar-password")
                        || ruta.equals("/estilos.css")
                        || ruta.equals("/login.css")
                        || ruta.equals("/cambiar-password.js")
                        || ruta.equals("/favicon.ico")
                        || ruta.equals("/logout");

                if (!rutaPermitida) {
                    response.sendRedirect("/cambiar-password");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

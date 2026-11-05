
package com.proyectoRequerimientos.prestamo_equipos.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CambioPasswordObligatorioFilter extends OncePerRequestFilter {

    private final CuentaPersonaRepository cuentaPersonaRepository;

    public CambioPasswordObligatorioFilter(
            CuentaPersonaRepository cuentaPersonaRepository) {
        this.cuentaPersonaRepository = cuentaPersonaRepository;
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

            var cuenta = cuentaPersonaRepository
                    .findByUsuarioIgnoreCase(autenticacion.getName());

            if (cuenta.isPresent()
                    && cuenta.get().isActivo()
                    && cuenta.get().isDebeCambiarPassword()) {

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

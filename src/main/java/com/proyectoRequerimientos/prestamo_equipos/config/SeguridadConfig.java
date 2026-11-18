
package com.proyectoRequerimientos.prestamo_equipos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;
import com.proyectoRequerimientos.prestamo_equipos.repository.CuentaPersonaRepository;
import com.proyectoRequerimientos.prestamo_equipos.security.CambioPasswordObligatorioFilter;

@Configuration
public class SeguridadConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SessionRegistry sessionRegistry() {
                return new SessionRegistryImpl();
        }

        @Bean
        public HttpSessionEventPublisher httpSessionEventPublisher() {
                return new HttpSessionEventPublisher();
        }

        @Bean
        public UserDetailsService userDetailsService(
                        EncargadoRepository encargadoRepository,
                        CuentaPersonaRepository cuentaPersonaRepository) {

                return usuario -> {

                        var encargado = encargadoRepository
                                        .findByUsuarioIgnoreCase(usuario)
                                        .filter(empleado -> empleado.isActivo())
                                        .filter(empleado -> !Boolean.FALSE.equals(
                                                        empleado.getCorreoVerificado()));

                        if (encargado.isPresent()) {

                                var empleado = encargado.get();

                                return User.withUsername(empleado.getUsuario())
                                                .password(empleado.getPasswordHash())
                                                .roles(empleado.getRol())
                                                .build();
                        }

                        var cuentaPersona = cuentaPersonaRepository
                                        .findByUsuarioIgnoreCase(usuario)
                                        .filter(cuenta -> cuenta.isActivo())
                                        .filter(cuenta -> cuenta.getPersona() != null)
                                        .filter(cuenta -> !Boolean.FALSE.equals(
                                                        cuenta.getPersona().getActivo()))
                                        .filter(cuenta -> !Boolean.FALSE.equals(
                                                        cuenta.getCorreoVerificado()));

                        if (cuentaPersona.isPresent()) {

                                var cuenta = cuentaPersona.get();

                                return User.withUsername(cuenta.getUsuario())
                                                .password(cuenta.getPasswordHash())
                                                .roles("ALUMNO")
                                                .build();
                        }

                        throw new UsernameNotFoundException(
                                        "Cuenta no encontrada o inactiva");
                };
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        CuentaPersonaRepository cuentaPersonaRepository,
                        EncargadoRepository encargadoRepository,
                        SessionRegistry sessionRegistry)
                        throws Exception {

                http.addFilterBefore(
                                new CambioPasswordObligatorioFilter(
                                                cuentaPersonaRepository,
                                                encargadoRepository),
                                AuthorizationFilter.class);

                http.authorizeHttpRequests(auth -> auth

                                // Páginas y recursos públicos.
                                .requestMatchers(
                                                "/login",
                                                "/activar-cuenta",
                                                "/olvide-password",
                                                "/restablecer-password",

                                                // Configuración del primer administrador.
                                                "/configuracion-inicial",
                                                "/api/configuracion-inicial",
                                                "/api/configuracion-inicial/**",

                                                "/api/cuentas/activar",
                                                "/api/cuentas/solicitar-recuperacion",
                                                "/api/cuentas/restablecer-password",

                                                "/estilos.css",
                                                "/login.css",

                                                "/activar-cuenta.js",
                                                "/olvide-password.js",
                                                "/restablecer-password.js",

                                                "/favicon.ico")
                                .permitAll()

                                // Administración de encargados y administradores: solo ADMIN.
                                .requestMatchers(
                                                "/encargados",
                                                "/encargados/**",
                                                "/api/encargados",
                                                "/api/encargados/**")
                                .hasRole("ADMIN")

                                // Consulta del estado de la cuenta del alumno.
                                .requestMatchers("/api/personas/mi-estado")
                                .hasRole("ALUMNO")

                                // Historial personal: solo alumnos.
                                .requestMatchers(
                                                "/mi-historial",
                                                "/api/prestamos/mi-historial")
                                .hasRole("ALUMNO")

                                // Operaciones administrativas de préstamos y bloqueos.
                                .requestMatchers(
                                                "/inventario",
                                                "/inventario/**",
                                                "/personas",
                                                "/personas/**",
                                                "/prestamos",
                                                "/prestamos/**",
                                                "/bloqueos",
                                                "/bloqueos/**",
                                                "/api/equipos",
                                                "/api/equipos/**",
                                                "/api/personas",
                                                "/api/personas/**",
                                                "/api/prestamos",
                                                "/api/prestamos/**")
                                .hasAnyRole("ADMIN", "ENCARGADO")

                                // Disponibilidad de equipos: solo alumnos.
                                .requestMatchers(
                                                "/disponibilidad",
                                                "/api/disponibilidad",
                                                "/api/disponibilidad/**")
                                .hasRole("ALUMNO")

                                // Funciones disponibles para cualquier usuario autenticado.
                                .requestMatchers(
                                                "/cambiar-password",
                                                "/api/mi-cuenta/**")
                                .authenticated()

                                .anyRequest().authenticated())

                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/inicio", true)
                                                .failureHandler((request, response, exception) -> {

                                                        if (exception instanceof SessionAuthenticationException) {
                                                                response.sendRedirect("/login?maxSessions");
                                                        } else {
                                                                response.sendRedirect("/login?error");
                                                        }

                                                })
                                                .permitAll())

                                /// Una sola sesión activa por usuario.
                                // Si inicia sesión nuevamente, la sesión anterior expira.
                                .sessionManagement(session -> session
                                                .maximumSessions(1)
                                                .maxSessionsPreventsLogin(false)
                                                .sessionRegistry(sessionRegistry)
                                                .expiredUrl("/login?expired"))

                                .logout(logout -> logout
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll());

                return http.build();
        }
}

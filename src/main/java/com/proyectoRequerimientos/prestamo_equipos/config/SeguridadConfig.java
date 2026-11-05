
package com.proyectoRequerimientos.prestamo_equipos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

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
    public UserDetailsService userDetailsService(
            EncargadoRepository encargadoRepository,
            CuentaPersonaRepository cuentaPersonaRepository) {

        return usuario -> {

            var encargado = encargadoRepository
                    .findByUsuarioIgnoreCase(usuario)
                    .filter(empleado -> empleado.isActivo());

            if (encargado.isPresent()) {

                var empleado = encargado.get();

                return User.withUsername(empleado.getUsuario())
                        .password(empleado.getPasswordHash())
                        .roles(empleado.getRol())
                        .build();
            }

            var cuentaPersona = cuentaPersonaRepository
                    .findByUsuarioIgnoreCase(usuario)
                    .filter(cuenta -> cuenta.isActivo());

            if (cuentaPersona.isPresent()) {

                var cuenta = cuentaPersona.get();

                String rol = cuenta.getPersona().getTipo()
                        .equalsIgnoreCase("Alumno") ? "ALUMNO" : "PROFESOR";

                return User.withUsername(cuenta.getUsuario())
                        .password(cuenta.getPasswordHash())
                        .roles(rol)
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
            EncargadoRepository encargadoRepository)
            throws Exception {

        http.addFilterBefore(
                new CambioPasswordObligatorioFilter(
                        cuentaPersonaRepository,
                        encargadoRepository),
                AuthorizationFilter.class);

        http.authorizeHttpRequests(auth -> auth

            .requestMatchers(
                "/login",
                "/estilos.css",
                "/login.css",
                "/favicon.ico"
            ).permitAll()

            .requestMatchers(
                "/encargados",
                "/encargados/**",
                "/api/encargados",
                "/api/encargados/**"
            ).hasRole("ADMIN")

            // Historial personal: solo alumnos y profesores.
            .requestMatchers(
                "/mi-historial",
                "/api/prestamos/mi-historial"
            ).hasAnyRole("ALUMNO", "PROFESOR")

            // Operaciones administrativas de préstamos.
            .requestMatchers(
                "/inventario",
                "/inventario/**",
                "/personas",
                "/personas/**",
                "/prestamos",
                "/prestamos/**",
                "/api/equipos",
                "/api/equipos/**",
                "/api/personas",
                "/api/personas/**",
                "/api/prestamos",
                "/api/prestamos/**"
            ).hasAnyRole("ADMIN", "ENCARGADO")

            .requestMatchers(
                "/disponibilidad",
                "/api/disponibilidad",
                "/api/disponibilidad/**"
            ).hasAnyRole("ALUMNO", "PROFESOR")

            .requestMatchers(
                "/cambiar-password",
                "/api/mi-cuenta/**"
            ).authenticated()

            .anyRequest().authenticated()
        )

        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/inicio", true)
            .failureUrl("/login?error")
            .permitAll()
        )

        .logout(logout -> logout
            .logoutSuccessUrl("/login?logout")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        );

        return http.build();
    }
}

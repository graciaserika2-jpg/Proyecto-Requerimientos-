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

import com.proyectoRequerimientos.prestamo_equipos.repository.EncargadoRepository;

@Configuration
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            EncargadoRepository repository) {

        return usuario -> repository
                .findByUsuarioIgnoreCase(usuario)
                .filter(empleado -> empleado.isActivo())
                .map(empleado -> User
                        .withUsername(empleado.getUsuario())
                        .password(empleado.getPasswordHash())
                        .roles(empleado.getRol())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Cuenta no encontrada o inactiva"));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                // Inicio de sesión y recursos públicos
                .requestMatchers(
                    "/login",
                    "/estilos.css",
                    "/login.css",
                    "/favicon.ico"
                ).permitAll()

                // Solo ADMIN puede administrar cuentas de encargados
                .requestMatchers(
                    "/encargados",
                    "/encargados/**",
                    "/api/encargados",
                    "/api/encargados/**"
                ).hasRole("ADMIN")

                // Operaciones del personal
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

                // Todas las demás rutas requieren autenticación
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
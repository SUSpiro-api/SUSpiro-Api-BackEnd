package com.app.suspiro.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.app.suspiro.service.UsuarioService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioService usuarioService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SenhaProvisoriaFilter senhaProvisoriaFilter;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(
            UsuarioService usuarioService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SenhaProvisoriaFilter senhaProvisoriaFilter,
            PasswordEncoder passwordEncoder) {

        this.usuarioService = usuarioService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.senhaProvisoriaFilter = senhaProvisoriaFilter;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(usuarioService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())


            .cors(Customizer.withDefaults())

            .authenticationProvider(authenticationProvider())

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/error",
                    "/auth/**",
                    "/acompanhamento",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs",
                    "/api-docs/**",
                    "/webjars/**"
                )
                .permitAll()

                .anyRequest()
                .authenticated()
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            .addFilterAfter(
                senhaProvisoriaFilter,
                JwtAuthenticationFilter.class
            );

        return http.build();
    }
}

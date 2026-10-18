package edu.unimagdalena.PulsePass.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration 
@EnableWebSecurity 
public class ControllerTestSecurityConfig {
       @Bean
    SecurityFilterChain controllerTestFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/events/published", "/api/artists/**", "/api/venues/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/events/*", "/api/events/by-artist").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/tickets").hasRole("USER")
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> res.sendError(401))
                .accessDeniedHandler((req, res, e) -> res.sendError(403)));
        return http.build();
    }
}

package edu.unimagdalena.PulsePass.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration 
@EnableWebSecurity 
@EnableMethodSecurity 
@EnableConfigurationProperties (JwtProperties.class)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            AuthenticationEntryPoint authEntryPoint,
            AccessDeniedHandler deniedHandler) throws Exception {

        http
            .csrf(csrf -> csrf.disable()) // API Bearer, sin cookies de autenticación
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(authEntryPoint)
                    .accessDeniedHandler(deniedHandler))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()
                    .requestMatchers(HttpMethod.GET,
                            "/api/events/published", "/api/artists/**", "/api/venues/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use")
                            .hasAnyRole("STAFF", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid")
                            .hasAnyRole("STAFF", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/events/*", "/api/events/by-artist").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/tickets").hasRole("USER")
                    .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
        PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 5.1: comprueba usuario y contraseña usando nuestro servicio y BCrypt
    @Bean
    AuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                              PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

        // 5.1: puerta de entrada que usará AuthService para el login
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
       return configuration.getAuthenticationManager();
    }

      // El filtro se crea aquí y no con @Component (ver explicación en la sección 3)
    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                    CustomUserDetailsService userDetailsService,
                                                    AuthenticationEntryPoint authEntryPoint) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService, authEntryPoint);
    }

    // Evita que Spring Boot lo registre una segunda vez fuera de la cadena de seguridad
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}

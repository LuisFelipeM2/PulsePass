package edu.unimagdalena.PulsePass.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter{
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // FR-SECAUTH-007: el token solo se lee del encabezado Authorization (nunca de la URL)
        List<String> headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));

        // Sin encabezado: se sigue y la cadena decide si la ruta es pública (14.3, advertencia 5)
        if (headers.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        // 17.1: dos encabezados Authorization es sospechoso, se rechaza
        if (headers.size() > 1) {
            reject(request, response);
            return;
        }

        String header = headers.get(0);
        if (!header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // 17.1: no se sobrescribe una autenticación ya establecida
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // FR-SECAUTH-006: firma, emisor, vencimiento y algoritmo se verifican aquí
            Long userId = jwtService.extractUserId(header.substring(7).trim());

            // FR-SECAUTH-009 / SEC011: el usuario y sus roles se vuelven a leer de la base
            UserDetails userDetails = userDetailsService.loadUserById(userId);

            // FR-SECAUTH-004: un usuario inactivo no entra aunque su token sea válido
            if (!userDetails.isEnabled()) {
                reject(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            // SEC008 / NFR-SEC002: token inválido, vencido o malformado = 401, nunca un 500
            reject(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response,
                new BadCredentialsException("Token inválido"));
    }
}

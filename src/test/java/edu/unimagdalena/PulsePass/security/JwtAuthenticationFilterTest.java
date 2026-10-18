package edu.unimagdalena.PulsePass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;

import edu.unimagdalena.PulsePass.domain.Role;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;

@ExtendWith (MockitoExtension.class)
public class JwtAuthenticationFilterTest {
    @Mock JwtService jwtService;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock AuthenticationEntryPoint entryPoint;
    @Mock FilterChain chain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, entryPoint);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SecurityPrincipal principal(boolean active) {
        return new SecurityPrincipal(1L, "andrea", "hash", active, Set.of(Role.USER));
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenThereIsNoHeader() throws Exception {
        // 17.1: sin Bearer
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldAuthenticateWhenBearerIsValid() throws Exception {
        // 17.1: Bearer válido
        request.addHeader("Authorization", "Bearer token-valido");
        SecurityPrincipal principal = principal(true);
        when(jwtService.extractUserId("token-valido")).thenReturn(1L);
        when(userDetailsService.loadUserById(1L)).thenReturn(principal);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isSameAs(principal);
    }

    @Test
    void shouldRejectWhenBearerIsInvalid() throws Exception {
        // 17.1: Bearer inválido. Responde 401 y no sigue la cadena.
        request.addHeader("Authorization", "Bearer malo");
        when(jwtService.extractUserId("malo")).thenThrow(new MalformedJwtException("malformado"));

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(eq(request), eq(response), any(AuthenticationException.class));
        verify(chain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldRejectWhenThereAreTwoAuthorizationHeaders() throws Exception {
        // 17.1: doble encabezado sospechoso
        request.addHeader("Authorization", "Bearer uno");
        request.addHeader("Authorization", "Bearer dos");

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(eq(request), eq(response), any(AuthenticationException.class));
        verify(chain, never()).doFilter(any(), any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldRejectInactiveUserEvenWithValidToken() throws Exception {
        // FR-SECAUTH-004 / T-SEC-016
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.extractUserId("token-valido")).thenReturn(1L);
        when(userDetailsService.loadUserById(1L)).thenReturn(principal(false));

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(eq(request), eq(response), any(AuthenticationException.class));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectWhenUserNoLongerExists() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(jwtService.extractUserId("token-valido")).thenReturn(1L);
        when(userDetailsService.loadUserById(1L)).thenThrow(new UsernameNotFoundException("no existe"));

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(eq(request), eq(response), any(AuthenticationException.class));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldNotOverwriteExistingAuthentication() throws Exception {
        // 17.1: no sobrescribir una autenticación ya establecida
        Authentication existing = UsernamePasswordAuthenticationToken.authenticated("otro", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer token-valido");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
    }
}

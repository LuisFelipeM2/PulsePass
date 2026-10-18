package edu.unimagdalena.PulsePass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Set;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import edu.unimagdalena.PulsePass.domain.Role;
import edu.unimagdalena.PulsePass.dto.request.LoginRequest;
import edu.unimagdalena.PulsePass.dto.response.AuthResponse;
import edu.unimagdalena.PulsePass.dto.response.AuthenticatedUserResponse;
import edu.unimagdalena.PulsePass.repository.UserCredentialRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;
import edu.unimagdalena.PulsePass.repository.UserRoleRepository;
import edu.unimagdalena.PulsePass.service.AuthService;
import edu.unimagdalena.PulsePass.service.UserService;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock UserService userService;
    @Mock UserRepository userRepository;
    @Mock UserCredentialRepository credentialRepository;
    @Mock UserRoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;

    private AuthService service;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties("pulsepass-api", "no-se-usa", Duration.ofMinutes(15));
        service = new AuthService(authenticationManager, jwtService, properties, userService,
                userRepository, credentialRepository, roleRepository, passwordEncoder);
    }

    @Test
    void shouldReturnTokenWhenCredentialsAreCorrect() {
        // 17.1: credenciales correctas
        SecurityPrincipal principal = new SecurityPrincipal(1L, "andrea", "hash", true, Set.of(Role.USER));
        Authentication authentication = UsernamePasswordAuthenticationToken
                .authenticated(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(1L)).thenReturn("token-falso");

        AuthResponse response = service.login(new LoginRequest("andrea", "clave"));

        assertThat(response.accessToken()).isEqualTo("token-falso");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
    }

    @Test
    void shouldNotIssueTokenWhenPasswordIsWrong() {
        // 17.1: password incorrecto, y el token solo se emite tras autenticar
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("mala"));

        assertThatThrownBy(() -> service.login(new LoginRequest("andrea", "incorrecta")))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldNotIssueTokenForInactiveUser() {
        // 17.1: usuario inactivo (FR-SECAUTH-004)
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("inactivo"));

        assertThatThrownBy(() -> service.login(new LoginRequest("andrea", "clave")))
                .isInstanceOf(DisabledException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldReturnMinimalDataForMe() {
        // FR-SECAUTH-010
        SecurityPrincipal principal = new SecurityPrincipal(1L, "andrea", "hash", true, Set.of(Role.USER));

        AuthenticatedUserResponse response = service.me(principal);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("andrea");
        assertThat(response.roles()).containsExactly("ROLE_USER");
    }
}

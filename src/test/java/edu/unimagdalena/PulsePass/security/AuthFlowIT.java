package edu.unimagdalena.PulsePass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import edu.unimagdalena.PulsePass.domain.Role;
import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.domain.UserCredential;
import edu.unimagdalena.PulsePass.dto.request.LoginRequest;
import edu.unimagdalena.PulsePass.dto.request.RegisterUserRequest;
import edu.unimagdalena.PulsePass.dto.response.AuthResponse;
import edu.unimagdalena.PulsePass.exception.DuplicateResourceException;
import edu.unimagdalena.PulsePass.repository.UserCredentialRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;
import edu.unimagdalena.PulsePass.repository.UserRoleRepository;
import edu.unimagdalena.PulsePass.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;

@SpringBootTest 
@Testcontainers 
public class AuthFlowIT {
     @Container 
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    // Clave y emisor exclusivos de pruebas (14.2)
    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("pulsepass.security.jwt.secret-base64",
                () -> Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded()));
        registry.add("pulsepass.security.jwt.issuer", () -> "pulsepass-test");
        registry.add("pulsepass.security.jwt.access-token-ttl", () -> "PT15M");
    }

    @Autowired AuthService authService;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired UserCredentialRepository credentialRepository;
    @Autowired UserRoleRepository roleRepository;

    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    // Orden de campos de tu RegisterUserRequest: username, email, password, firstName, lastName, phone, city, birthDate
    private RegisterUserRequest request(String username, String email) {
        return new RegisterUserRequest(username, email, PASSWORD, "Andrea", "Perez",
                "3001234567", "Santa Marta", LocalDate.of(2000, 5, 10));
    }

    @Test
    void shouldRegisterWithBcryptHashAndUserRole() {
        // UC-SEC-01 / FR-SECAUTH-001
        authService.register(request("andrea1", "andrea1@email.com"));

        User user = userRepository.findByUsername("andrea1").orElseThrow();
        UserCredential credential = credentialRepository.findById(user.getId()).orElseThrow();
        assertThat(credential.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(roleRepository.findByIdUserId(user.getId()))
                .extracting(r -> r.getId().getRole())
                .containsExactly(Role.USER);
    }

    @Test
    void shouldLoginWithUsernameAndWithEmail() {
        // UC-SEC-02 / FR-SECAUTH-002
        authService.register(request("andrea2", "andrea2@email.com"));
        Long id = userRepository.findByUsername("andrea2").orElseThrow().getId();

        AuthResponse byUsername = authService.login(new LoginRequest("andrea2", PASSWORD));
        AuthResponse byEmail = authService.login(new LoginRequest("andrea2@email.com", PASSWORD));

        assertThat(jwtService.extractUserId(byUsername.accessToken())).isEqualTo(id);
        assertThat(jwtService.extractUserId(byEmail.accessToken())).isEqualTo(id);
        assertThat(byUsername.expiresIn()).isEqualTo(900);
    }

    @Test
    void shouldRejectWrongPassword() {
        // FR-SECAUTH-003
        authService.register(request("andrea3", "andrea3@email.com"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea3", "incorrecta-incorrecta")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldNotLeaveCredentialsWhenRegistrationFails() {
        // NFR-SEC004: un registro duplicado no deja credenciales de más
        authService.register(request("andrea4", "andrea4@email.com"));
        long before = credentialRepository.count();

        assertThatThrownBy(() -> authService.register(request("andrea4", "otro@email.com")))
                .isInstanceOf(DuplicateResourceException.class);

        assertThat(credentialRepository.count()).isEqualTo(before);
    }
}

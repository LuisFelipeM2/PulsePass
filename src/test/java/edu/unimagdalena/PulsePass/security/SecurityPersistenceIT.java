package edu.unimagdalena.PulsePass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.domain.UserCredential;
import edu.unimagdalena.PulsePass.repository.UserCredentialRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;

@DataJpaTest 
@AutoConfigureTestDatabase (replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers 
public class SecurityPersistenceIT {
    @Container 
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository userRepository;
    @Autowired UserCredentialRepository credentialRepository;

    private User saveUser(String username) {
        return userRepository.saveAndFlush(User.builder()
                .username(username)
                .email(username + "@test.com")
                .active(true)
                .build());
    }

    @Test
    void shouldCreateSecurityTablesWithFlywayMigration() {
        // 17.3: la migración V4 corre y crea las dos tablas
        Integer tables = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_name IN ('user_credentials', 'user_roles')
                """, Integer.class);
        assertThat(tables).isEqualTo(2);
    }

    @Test
    void shouldPersistBcryptHashInsteadOfPlainPassword() {
        // SEC003 / 17.3: se guarda el hash BCrypt, nunca la contraseña
        User user = saveUser("andrea");
        String raw = "UnaFraseSeguraLarga2026!";
        String hash = new BCryptPasswordEncoder().encode(raw);

        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setPasswordHash(hash);
        credentialRepository.saveAndFlush(credential);

        UserCredential saved = credentialRepository.findById(user.getId()).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo(raw);
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches(raw, saved.getPasswordHash())).isTrue();
    }

    @Test
    void shouldRejectDuplicateRoleForSameUser() {
        // 9.1 / 17.3: la clave (user_id, role) no admite repetidos
        User user = saveUser("carlos");
        jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'USER')", user.getId());

        assertThatThrownBy(() ->
                jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'USER')", user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectRoleOutsideAllowedValues() {
        // 9.2: el rol está restringido a USER, STAFF o ADMIN
        User user = saveUser("laura");

        assertThatThrownBy(() ->
                jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, 'SUPERADMIN')", user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldLeaveLegacyUserWithoutCredentials() {
        // 9.2: un usuario preexistente queda sin credencial, así que no puede iniciar sesión
        User legacy = saveUser("miguel");

        assertThat(credentialRepository.findById(legacy.getId())).isEmpty();
    }
}

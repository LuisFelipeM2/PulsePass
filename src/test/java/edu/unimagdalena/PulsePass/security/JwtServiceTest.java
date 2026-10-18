package edu.unimagdalena.PulsePass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;

public class JwtServiceTest {
     // Clave exclusiva de pruebas, generada al azar en cada ejecución (12.2, 14.2)
    private static final SecretKey KEY = Jwts.SIG.HS256.key().build();
    private static final String SECRET = Encoders.BASE64.encode(KEY.getEncoded());
    private static final String ISSUER = "pulsepass-test";

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService(new JwtProperties(ISSUER, SECRET, Duration.ofMinutes(15)));
    }

    @Test
    void shouldGenerateTokenWithRequiredClaims() {
        // FR-SECAUTH-005: sub, iss, iat, exp y jti, con vida de 15 minutos
        String token = service.generateToken(1L);

        Claims claims = Jwts.parser().verifyWith(KEY).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isBetween(899_000L, 901_000L);
    }

    @Test
    void shouldExtractUserIdFromValidToken() {
        assertThat(service.extractUserId(service.generateToken(7L))).isEqualTo(7L);
    }

    @Test
    void shouldRejectExpiredToken() {
        // FR-SECAUTH-006: token vencido
        Instant past = Instant.now().minusSeconds(3600);
        String expired = Jwts.builder()
                .subject("1").issuer(ISSUER)
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plusSeconds(60)))
                .id(UUID.randomUUID().toString())
                .signWith(KEY, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> service.extractUserId(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldRejectTokenWithWrongIssuer() {
        // FR-SECAUTH-006: emisor erróneo
        JwtService other = new JwtService(new JwtProperties("otro-emisor", SECRET, Duration.ofMinutes(15)));

        assertThatThrownBy(() -> service.extractUserId(other.generateToken(1L)))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTamperedToken() {
        // FR-SECAUTH-006: manipulado. Se cambia un carácter de la firma.
        String token = service.generateToken(1L);
        String[] parts = token.split("\\.");
        char[] signature = parts[2].toCharArray();
        signature[10] = signature[10] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + new String(signature);

        assertThatThrownBy(() -> service.extractUserId(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenSignedWithAnotherKey() {
        SecretKey otherKey = Jwts.SIG.HS256.key().build();
        String foreign = Jwts.builder().subject("1").issuer(ISSUER)
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(otherKey, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> service.extractUserId(foreign))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectUnsignedToken() {
        // 12.2: no se permite alg=none ni token sin firma
        String unsigned = Jwts.builder().subject("1").issuer(ISSUER)
                .expiration(Date.from(Instant.now().plusSeconds(60))).compact();

        assertThatThrownBy(() -> service.extractUserId(unsigned))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectEmptyToken() {
        // 17.1: token vacío. JJWT lanza JwtException o IllegalArgumentException; el filtro atrapa ambas.
        assertThatThrownBy(() -> service.extractUserId(""))
                .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }

    @Test
    void shouldFailAtStartupWhenKeyIsWeak() {
        // 12.2: la app debe fallar al arrancar si la clave es insuficiente (menos de 32 bytes)
        String weak = Encoders.BASE64.encode(new byte[16]);

        assertThatThrownBy(() -> new JwtService(new JwtProperties(ISSUER, weak, Duration.ofMinutes(15))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldFailAtStartupWhenKeyIsMissing() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(ISSUER, null, Duration.ofMinutes(15))))
                .isInstanceOf(IllegalStateException.class);
    }
}

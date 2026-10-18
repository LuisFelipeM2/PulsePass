package edu.unimagdalena.PulsePass.security;

import java.sql.Date;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service 
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        // 12.2: la app debe fallar al arrancar si la clave falta o es insuficiente
        if (properties.secretBase64() == null || properties.secretBase64().isBlank()) {
            throw new IllegalStateException("Falta PULSEPASS_JWT_SECRET_BASE64");
        }
        byte[] keyBytes = Decoders.BASE64.decode(properties.secretBase64());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("La clave JWT debe tener al menos 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // FR-SECAUTH-005: sub, iss, iat, exp y jti, firmado con HS256
    public String generateToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // FR-SECAUTH-006: rechaza expirado, manipulado, issuer erróneo,
    // malformado o algoritmo inesperado. Lanza JwtException si algo falla.
    public Long extractUserId(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token);

        if (!"HS256".equals(jws.getHeader().getAlgorithm())) {
            throw new JwtException("Algoritmo inesperado");
        }
        Claims claims = jws.getPayload();
        if (claims.getExpiration() == null
                || claims.getSubject() == null
                || claims.getSubject().isBlank()) {
            throw new JwtException("Faltan claims obligatorios");
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("sub inválido");
        }
    }
}

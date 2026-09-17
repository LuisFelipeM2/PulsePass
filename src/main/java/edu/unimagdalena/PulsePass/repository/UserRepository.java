package edu.unimagdalena.PulsePass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.unimagdalena.PulsePass.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    // FR-USR-002: "username y email deben ser unicos." / FR-USR-001:
    // "El usuario puede persistirse y recuperarse."
    // Mecanismo: Query Method.
    Optional<User> findByUsername(String username);

    // Seccion 14, fila "Buscar usuario por email ignorando mayusculas":
    // Mecanismo: Query Method. "IgnoreCase" le dice a Spring que genere
    // el SQL con LOWER(email)
    
    Optional<User> findByEmailIgnoreCase(String email);
 }

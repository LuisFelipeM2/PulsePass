package edu.unimagdalena.PulsePass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.unimagdalena.PulsePass.domain.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> { 
    // FR-USR-003 / FR-USR-004: "Cada usuario puede poseer un unico UserProfile."
    // "Los datos se recuperan desde la relacion User 1:1 UserProfile."
    // Mecanismo: Query Method navegando relacion 
    // Devuelve Optional porque un usuario podria no tener perfil todavia
    // (BR-004 dice "maximo un perfil", no "obligatoriamente uno").
    Optional<UserProfile> findByUser_Id(Long userId);
 
    // Alternativa util para pruebas/consultas por identificador de negocio
    // en vez del ID tecnico, navegando User -> username.
    Optional<UserProfile> findByUser_Username(String username);
}
package edu.unimagdalena.PulsePass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.unimagdalena.PulsePass.domain.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> { 

    // FR-VEN-002 / FR-VEN-001: recuperar un venue por su codigo de negocio.
    // Mecanismo: Query Method (seccion 14).
    Optional<Venue> findByCode(String code);
}

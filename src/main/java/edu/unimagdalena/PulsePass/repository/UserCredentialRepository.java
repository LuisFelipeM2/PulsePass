package edu.unimagdalena.PulsePass.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.unimagdalena.PulsePass.domain.UserCredential;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    
}

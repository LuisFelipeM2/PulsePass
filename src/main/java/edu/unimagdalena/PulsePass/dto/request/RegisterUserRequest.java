package edu.unimagdalena.PulsePass.dto.request;

import java.time.LocalDate;

// Sección 19: RegisterUserRequest
// NFR-003: DTO inmutable implementado como record.

public record RegisterUserRequest(
        String username,    
        String email,       
        String firstName,
        String lastName,
        String phone,
        String city,
        LocalDate birthDate 
) {
    // BR-USER-003: el request no incluye "active". El servicio crea todo
    // usuario nuevo con active = true, el cliente no lo controla.
}

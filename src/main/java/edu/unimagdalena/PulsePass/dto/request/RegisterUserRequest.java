package edu.unimagdalena.PulsePass.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Sección 19: RegisterUserRequest
// NFR-003: DTO inmutable implementado como record.

public record RegisterUserRequest(
        @NotBlank (message = "Username is required")
        String username,

        @NotBlank(message = "Email is required")
        @Email (message = "Email format is invalid")
        String email,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        String phone,

        String city,

        @NotNull (message = "Birth date is required")
        LocalDate birthDate
) {
    // BR-USER-003: el request no incluye "active". El servicio crea todo
    // usuario nuevo con active = true, el cliente no lo controla.
}

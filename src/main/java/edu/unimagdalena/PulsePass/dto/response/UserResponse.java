package edu.unimagdalena.PulsePass.dto.response;

// Sección 18: tipo de retorno de UserService.
// NFR-003: DTO inmutable implementado como record.
// Los campos no están definidos en el PRD. Se excluyen phone, city y birthDate (datos personales).
public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean active              // BR-USER-003: un usuario recién registrado sale con active = true
) {}

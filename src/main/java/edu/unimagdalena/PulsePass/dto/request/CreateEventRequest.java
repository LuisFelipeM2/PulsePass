package edu.unimagdalena.PulsePass.dto.request;

import java.time.LocalDateTime;

import edu.unimagdalena.PulsePass.domain.EventCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Sección 12: CreateEventRequest, DTO de entrada de EventService.create.
// NFR-003: DTO inmutable implementado como record.
public record CreateEventRequest(
        @NotBlank (message = "Event code is required")
        String eventCode,

        @NotBlank(message = "Name is required")
        String name,

        // Sección 11.1: longitud máxima definida.
        @Size (max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull (message = "Category is required")
        EventCategory category,

        @NotNull(message = "Event date is required")
        LocalDateTime eventDate,

        // Sección 11.1 / BR-EVENT-006: 0 significa sin restricción de edad.
        @NotNull(message = "Minimum age is required")
        @Min (value = 0, message = "Minimum age must be 0 or greater")
        Integer minimumAge,

        @NotBlank(message = "Venue code is required")
        String venueCode
) {
    // BR-EVENT-005: el request no incluye "status". Todo evento nuevo inicia
    // en DRAFT y lo asigna el servicio, el cliente no lo controla.
}
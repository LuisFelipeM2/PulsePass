package edu.unimagdalena.PulsePass.dto.request;

import edu.unimagdalena.PulsePass.domain.TicketType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Sección 22: PurchaseTicketRequest
// NFR-003: DTO inmutable implementado como record.

public record PurchaseTicketRequest(
        @NotBlank (message = "User email is required")
        @Email (message = "User email format is invalid")
        String userEmail,

        @NotBlank(message = "Event code is required")
        String eventCode,

        @NotNull (message = "Ticket type is required")
        TicketType type
) {
}
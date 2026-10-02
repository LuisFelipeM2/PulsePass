package edu.unimagdalena.PulsePass.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import edu.unimagdalena.PulsePass.domain.TicketStatus;
import edu.unimagdalena.PulsePass.domain.TicketType;

// Sección 23: TicketResponse
// NFR-003: DTO inmutable implementado como record.
// Sección 23: no expone directamente User ni Event.
public record TicketResponse(
        Long id,
        String ticketCode,
        TicketType type,
        BigDecimal price,           // Sección 28: el precio usa BigDecimal; BR-TICKET-009: nunca negativo
        TicketStatus status,        // Sección 27: una compra válida sale como PAID
        LocalDateTime purchaseDate,
        String userEmail,           
        String eventCode,           
        String eventName            
) {}

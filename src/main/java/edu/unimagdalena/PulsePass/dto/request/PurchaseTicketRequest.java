package edu.unimagdalena.PulsePass.dto.request;

import edu.unimagdalena.PulsePass.domain.TicketType;

// Sección 22: PurchaseTicketRequest
// NFR-003: DTO inmutable implementado como record.

public record PurchaseTicketRequest(
        String userEmail,  
        String eventCode,  
        TicketType type  
) {
    // Sección 22: el precio no viene del cliente. El request no tiene campo
    // "price"; la estrategia de precio del sistema lo calcula en la compra.
}

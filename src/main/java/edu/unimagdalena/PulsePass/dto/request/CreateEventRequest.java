package edu.unimagdalena.PulsePass.dto.request;

import java.time.LocalDateTime;

import edu.unimagdalena.PulsePass.domain.EventCategory;

// Sección 12: CreateEventRequest, DTO de entrada de EventService.create.
// NFR-003: DTO inmutable implementado como record.
public record CreateEventRequest(
        String eventCode,        
        String name,
        String description,
        EventCategory category,
        LocalDateTime eventDate, 
        Integer minimumAge,      
        String venueCode         
) {
    // BR-EVENT-005: el request no incluye "status". Todo evento nuevo inicia
    // en DRAFT y lo asigna el servicio, el cliente no lo controla.
}
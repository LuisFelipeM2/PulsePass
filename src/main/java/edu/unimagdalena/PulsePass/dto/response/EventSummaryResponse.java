package edu.unimagdalena.PulsePass.dto.response;

import java.time.LocalDateTime;

import edu.unimagdalena.PulsePass.domain.EventCategory;
import edu.unimagdalena.PulsePass.domain.EventStatus;

// Sección 11.1: tipo de retorno de findPublishedEvents() y findByArtist().
// SRV-001: el contrato público retorna DTOs, nunca la entidad Event.
// NFR-003: DTO inmutable implementado como record.
// Los campos no están definidos en el PRD; es una versión corta de EventResponse para listados.
public record EventSummaryResponse(
        Long id,
        String eventCode,
        String name,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        String venueName            
) {}

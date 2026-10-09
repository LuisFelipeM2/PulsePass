package edu.unimagdalena.PulsePass.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import edu.unimagdalena.PulsePass.domain.EventCategory;
import edu.unimagdalena.PulsePass.domain.EventStatus;

// Sección 13: EventResponse
// NFR-003: DTO inmutable implementado como record.
// Sección 13: no expone directamente Venue, Set<Artist> ni List<Ticket>.
public record EventResponse(
        Long id,
        String eventCode,
        String name,
        String description,
        EventCategory category,
        EventStatus status,         // BR-EVENT-005: un evento recién creado sale como DRAFT
        LocalDateTime eventDate,
        Integer minimumAge,
        String venueCode,          
        String venueName,           
        List<ArtistResponse> artists // Sección 13: artistas como DTOs, no como entidades
) {}

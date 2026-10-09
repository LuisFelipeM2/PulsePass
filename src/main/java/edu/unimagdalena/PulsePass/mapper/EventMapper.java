package edu.unimagdalena.PulsePass.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.unimagdalena.PulsePass.domain.Event;
import edu.unimagdalena.PulsePass.dto.response.EventResponse;
import edu.unimagdalena.PulsePass.dto.response.EventSummaryResponse;


// Sección 33: Event → EventResponse 
// componentModel = "spring": el mapper es un bean inyectable por constructor (SRV-002).

@Mapper (componentModel = "spring")
public interface EventMapper {
    @Mapping (target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    EventResponse toResponse(Event event);

 // Sección 33 / 37:
    @Mapping(target = "venueName", source = "venue.name")
    EventSummaryResponse toSummary(Event event);
}


package edu.unimagdalena.PulsePass.mapper;

import org.mapstruct.Mapper;

import edu.unimagdalena.PulsePass.domain.Venue;
import edu.unimagdalena.PulsePass.dto.response.VenueResponse;

// Sección 33: Venue → VenueResponse con MapStruct.
// componentModel = "spring": el mapper es un bean inyectable por constructor (SRV-002).

@Mapper (componentModel = "spring")
public interface VenueMapper {
    VenueResponse toResponse(Venue venue);
}

package edu.unimagdalena.PulsePass.mapper;

import org.mapstruct.Mapper;

import edu.unimagdalena.PulsePass.domain.Artist;
import edu.unimagdalena.PulsePass.dto.response.ArtistResponse;

// Sección 33: Artist → ArtistResponse con MapStruct.
// componentModel = "spring": el mapper es un bean inyectable por constructor (SRV-002).

@Mapper (componentModel = "spring")
public interface ArtistMapper {
    ArtistResponse toResponse(Artist artist);
}

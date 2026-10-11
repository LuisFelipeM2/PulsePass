package edu.unimagdalena.PulsePass.dto.response;



// NFR-003: DTO inmutable implementado como record.

public record VenueResponse(
        Long id,
        String code,
        String name,
        String city,
        String address,
        Integer capacity,
        Boolean active
) {}
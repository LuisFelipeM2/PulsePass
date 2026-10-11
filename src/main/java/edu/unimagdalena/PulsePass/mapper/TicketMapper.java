package edu.unimagdalena.PulsePass.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.unimagdalena.PulsePass.domain.Ticket;
import edu.unimagdalena.PulsePass.dto.response.TicketResponse;

// Sección 34: TicketMapper, mapeo de Ticket → TicketResponse.
// componentModel = "spring": el mapper es un bean inyectable por constructor (SRV-002).

@Mapper (componentModel = "spring")
public interface TicketMapper {

    // Sección 34: ticket.user.email → userEmail
    // Sección 34: ticket.event.eventCode → eventCode
    // Sección 34: ticket.event.name → eventName
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "eventCode", source = "event.eventCode")
    @Mapping(target = "eventName", source = "event.name")
    TicketResponse toResponse(Ticket ticket);
}

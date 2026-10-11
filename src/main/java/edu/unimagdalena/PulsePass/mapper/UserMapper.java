package edu.unimagdalena.PulsePass.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.dto.response.UserResponse;

// Sección 33: User → UserResponse con MapStruct.
// componentModel = "spring": el mapper es un bean inyectable por constructor (SRV-002).

@Mapper (componentModel = "spring")
public interface UserMapper {
    
    // UserResponse no incluye phone, city, birthDate, tickets ni profile (SRV-001).
    @Mapping (target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    UserResponse toResponse(User user);
}

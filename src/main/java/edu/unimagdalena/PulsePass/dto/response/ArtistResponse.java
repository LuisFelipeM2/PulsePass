package edu.unimagdalena.PulsePass.dto.response;

// Sección 17: tipo de retorno de ArtistService.
// NFR-003: DTO inmutable implementado como record.
// Los campos no están definidos en el PRD; son los campos simples de la entidad.
// No incluye "events" SRV-001 prohíbe exponer entidades.
public record ArtistResponse(
        Long id,
        String stageName,
        String country,
        String genre,
        Boolean active
) {}
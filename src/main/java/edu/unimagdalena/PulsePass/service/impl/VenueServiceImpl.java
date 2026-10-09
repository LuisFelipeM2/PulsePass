package edu.unimagdalena.PulsePass.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.unimagdalena.PulsePass.domain.Venue;
import edu.unimagdalena.PulsePass.dto.response.VenueResponse;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.mapper.VenueMapper;
import edu.unimagdalena.PulsePass.repository.VenueRepository;
import edu.unimagdalena.PulsePass.service.VenueService;
import org.springframework.transaction.annotation.Transactional;

// SRV-003: cada servicio principal tiene una interfaz y una implementación.

@Service 
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    public VenueServiceImpl(VenueRepository venueRepository, VenueMapper venueMapper) {
        this.venueRepository = venueRepository;
        this.venueMapper = venueMapper;
    }

    // FR-SVC-001 (sección 40): consultar venue por código.
    // SRV-004 / sección 38: lectura → @Transactional(readOnly = true). 
    @Override
    @Transactional(readOnly = true)
    public VenueResponse findByCode(String code) {

        Venue venue = venueRepository.findByCode(code)
                // BR-VENUE-001: si el venue solicitado no existe, se lanza
                // ResourceNotFoundException (sección 35: "el recurso no existe").
                .orElseThrow(() -> new ResourceNotFoundException("Venue", code));

        return venueMapper.toResponse(venue);
    }

    // FR-SVC-002 (sección 40): consultar venues activos.
    @Override
    @Transactional(readOnly = true)
    public List<VenueResponse> findActiveVenues() {

        // BR-VENUE-002: findActiveVenues() solo retorna venues con active = true.
        return venueRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(venueMapper::toResponse)
                .toList();
    }
}
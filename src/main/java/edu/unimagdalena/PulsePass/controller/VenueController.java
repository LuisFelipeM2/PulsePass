package edu.unimagdalena.PulsePass.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.unimagdalena.PulsePass.dto.response.VenueResponse;
import edu.unimagdalena.PulsePass.service.VenueService;

// CTRL-009: controller delgado, solo delega en el Service.
@RestController 
@RequestMapping ("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    // FR-CTRL-VEN-001: 200 si existe; si no, el Service lanza la excepción y el handler responde 404.
    @GetMapping ("/{code}")
    public ResponseEntity<VenueResponse> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(venueService.findByCode(code));
    }

    // FR-CTRL-VEN-002: venues activos.
    @GetMapping("/active")
    public ResponseEntity<List<VenueResponse>> findActiveVenues() {
        return ResponseEntity.ok(venueService.findActiveVenues());
    }
}
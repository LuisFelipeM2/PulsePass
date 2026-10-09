package edu.unimagdalena.PulsePass.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unimagdalena.PulsePass.dto.request.CreateEventRequest;
import edu.unimagdalena.PulsePass.dto.response.EventResponse;
import edu.unimagdalena.PulsePass.dto.response.EventSummaryResponse;
import edu.unimagdalena.PulsePass.service.EventService;
import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // FR-CTRL-EVT-001: 201 Created. @Valid hace que Bean Validation corra antes del Service (CTRL-005).
    @PostMapping 
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    // FR-CTRL-EVT-003: eventos publicados.
    @GetMapping ("/published")
    public ResponseEntity<List<EventSummaryResponse>> findPublishedEvents() {
        return ResponseEntity.ok(eventService.findPublishedEvents());
    }

    // FR-CTRL-EVT-006: búsqueda por nombre artístico.
    @GetMapping("/by-artist")
    public ResponseEntity<List<EventSummaryResponse>> findByArtist(@RequestParam String stageName) {
        return ResponseEntity.ok(eventService.findByArtist(stageName));
    }

    // FR-CTRL-EVT-002: 200 si existe, 404 si no.
    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> findByCode(@PathVariable String eventCode) {
        return ResponseEntity.ok(eventService.findByCode(eventCode));
    }

    // FR-CTRL-EVT-004: 200 si la transición es válida; 409 si el Service rechaza por regla de negocio.
    @PatchMapping ("/{eventCode}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable String eventCode) {
        return ResponseEntity.ok(eventService.publish(eventCode));
    }

    // FR-CTRL-EVT-005: 200 si se asocia; 409 si ya estaba asociado o el evento está CANCELLED/FINISHED.
    @PostMapping("/{eventCode}/artists/{artistId}")
    public ResponseEntity<EventResponse> addArtist(@PathVariable String eventCode,
                                                   @PathVariable Long artistId) {
        return ResponseEntity.ok(eventService.addArtist(eventCode, artistId));
    }
}
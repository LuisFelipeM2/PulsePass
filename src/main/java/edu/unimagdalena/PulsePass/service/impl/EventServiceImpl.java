package edu.unimagdalena.PulsePass.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.unimagdalena.PulsePass.domain.Artist;
import edu.unimagdalena.PulsePass.domain.Event;
import edu.unimagdalena.PulsePass.domain.EventStatus;
import edu.unimagdalena.PulsePass.domain.Venue;
import edu.unimagdalena.PulsePass.dto.request.CreateEventRequest;
import edu.unimagdalena.PulsePass.dto.response.EventResponse;
import edu.unimagdalena.PulsePass.dto.response.EventSummaryResponse;
import edu.unimagdalena.PulsePass.exception.BusinessRuleException;
import edu.unimagdalena.PulsePass.exception.DuplicateResourceException;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.mapper.EventMapper;
import edu.unimagdalena.PulsePass.repository.ArtistRepository;
import edu.unimagdalena.PulsePass.repository.EventRepository;
import edu.unimagdalena.PulsePass.repository.VenueRepository;
import edu.unimagdalena.PulsePass.service.EventService;

// SRV-003: implementación de EventService.
// SRV-004: escrituras con @Transactional, lecturas con readOnly = true.
@Service 
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(EventRepository eventRepository,
                            VenueRepository venueRepository,
                            ArtistRepository artistRepository,
                            EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
    }

    // FR-SVC-003: crear evento (sección 14).
    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {

        // BR-EVENT-001: no puede existir otro evento con el mismo eventCode.
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event already exists: " + request.eventCode());
        }

        // BR-EVENT-002: el venue debe existir; si no, ResourceNotFoundException.
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", request.venueCode()));

        // BR-EVENT-003: no se puede crear un evento en un venue inactivo.
        if (!venue.getActive()) {
            throw new BusinessRuleException("Venue is not active: " + venue.getCode());
        }
        // BR-EVENT-004: la fecha del evento debe ser futura al momento de crearlo.
        if (!request.eventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Event date must be in the future.");
        }

        // BR-EVENT-006: minimumAge >= 0 (0 = sin restricción de edad).
        // Si el request no trae edad mínima (null), se toma como 0.
        int minimumAge = request.minimumAge() == null ? 0 : request.minimumAge();
        if (minimumAge < 0) {
            throw new BusinessRuleException("Minimum age cannot be negative.");
        }

        Event event = Event.builder()
                .eventCode(request.eventCode())
                .name(request.name())
                .description(request.description())
                .category(request.category())
                // BR-EVENT-005: todo evento nuevo inicia en DRAFT; el request no lo controla.
                .status(EventStatus.DRAFT)
                .eventDate(request.eventDate())
                .minimumAge(minimumAge)
                .venue(venue)
                .build();

        return eventMapper.toResponse(eventRepository.save(event));
    }
 // FR-SVC-004: consultar evento por código.
    @Override
    @Transactional (readOnly = true)
    public EventResponse findByCode(String eventCode) {
        return eventMapper.toResponse(findEventOrThrow(eventCode));
    }

    // FR-SVC-005: consultar eventos publicados, ordenados por fecha.
    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }
    // FR-SVC-006: publicar evento, DRAFT → PUBLISHED (sección 15).
    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = findEventOrThrow(eventCode);

        // BR-EVENT-007: solo se publica un evento en estado DRAFT
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published. Current status: " + event.getStatus());
        }

        // BR-EVENT-008: el evento debe seguir teniendo fecha futura.
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Event date must be in the future.");
        }

        // BR-EVENT-009: el venue debe continuar activo.
        if (!event.getVenue().getActive()) {
            throw new BusinessRuleException("Venue is not active: " + event.getVenue().getCode());
        }

        event.setStatus(EventStatus.PUBLISHED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    // FR-SVC-007: asociar artista a evento (sección 16).
    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = findEventOrThrow(eventCode);

        // BR-ARTIST-001: si el artista no existe, ResourceNotFoundException.
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist", artistId));

        // BR-EVENT-011: no se agregan artistas a eventos CANCELLED ni FINISHED.
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException(
                    "Cannot add artists to an event with status " + event.getStatus());
        }
        // BR-EVENT-010: no se asocia dos veces el mismo artista al evento.
        // Se compara por id porque la entidad Artist no define equals.
        boolean alreadyAssociated = event.getArtists().stream()
                .anyMatch(existing -> Objects.equals(existing.getId(), artistId));
        if (alreadyAssociated) {
            throw new DuplicateResourceException("Artist already associated with event: " + eventCode);
        }

        event.getArtists().add(artist);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    // FR-SVC-008: consultar eventos por artista.
    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return artistRepository.findEventsByArtistStageName(stageName)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    // Evento inexistente → ResourceNotFoundException ("Event not found: CMF-2026", sección 35).
    // Sección 36: Optional con orElseThrow, nunca get() sin comprobar.
    private Event findEventOrThrow(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventCode));
    }
}
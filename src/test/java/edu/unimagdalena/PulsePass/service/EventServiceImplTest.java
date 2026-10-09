package edu.unimagdalena.PulsePass.service;

import edu.unimagdalena.PulsePass.domain.Artist;
import edu.unimagdalena.PulsePass.domain.Event;
import edu.unimagdalena.PulsePass.domain.EventCategory;
import edu.unimagdalena.PulsePass.domain.EventStatus;
import edu.unimagdalena.PulsePass.domain.Venue;
import edu.unimagdalena.PulsePass.dto.request.CreateEventRequest;
import edu.unimagdalena.PulsePass.dto.response.EventResponse;
import edu.unimagdalena.PulsePass.exception.BusinessRuleException;
import edu.unimagdalena.PulsePass.exception.DuplicateResourceException;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.mapper.EventMapper;
import edu.unimagdalena.PulsePass.repository.ArtistRepository;
import edu.unimagdalena.PulsePass.repository.EventRepository;
import edu.unimagdalena.PulsePass.repository.VenueRepository;
import edu.unimagdalena.PulsePass.service.impl.EventServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    private static final String EVENT_CODE = "CMF-2026";
    private static final String VENUE_CODE = "VEN-SMR-01";
    private static final Long ARTIST_ID = 10L;
    private static final LocalDateTime FUTURE_DATE = LocalDateTime.now().plusDays(30);
    private static final LocalDateTime PAST_DATE = LocalDateTime.now().minusDays(1);

    @Mock
    private EventRepository eventRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private EventMapper eventMapper;
    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    @InjectMocks
    private EventServiceImpl eventService;

        // --- Datos de prueba 

    private Venue venue(boolean active) {
        return Venue.builder()
                .id(1L)
                .code(VENUE_CODE)
                .name("Marina Convention Center")
                .city("Santa Marta")
                .capacity(3)
                .active(active)
                .build();
    }

    private Event event(EventStatus status, Venue venue) {
        return Event.builder()
                .id(1L)
                .eventCode(EVENT_CODE)
                .name("Caribbean Music Fest 2026")
                .category(EventCategory.MUSIC)
                .status(status)
                .eventDate(FUTURE_DATE)
                .minimumAge(18)
                .venue(venue)
                .build();
    }

    private Artist artist() {
        return Artist.builder()
                .id(ARTIST_ID)
                .stageName("Solar Beat")
                .active(true)
                .build();
    }

    private CreateEventRequest request(LocalDateTime date) {
        return new CreateEventRequest(EVENT_CODE, "Caribbean Music Fest 2026", null,
                EventCategory.MUSIC, date, 18, VENUE_CODE);
    }

    private EventResponse response(EventStatus status) {
        return new EventResponse(1L, EVENT_CODE, "Caribbean Music Fest 2026", null,
                EventCategory.MUSIC, status, FUTURE_DATE, 18, VENUE_CODE,
                "Marina Convention Center", List.of());
    }

    // TEST-EVENT-001: evento existente → retorna DTO.
    @Test
    void findByCode_existingEvent_returnsDto() {
        Event event = event(EventStatus.PUBLISHED, venue(true));
        EventResponse response = response(EventStatus.PUBLISHED);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(response);

        EventResponse result = eventService.findByCode(EVENT_CODE);

        assertThat(result).isEqualTo(response);
        verify(eventRepository).findByEventCode(eq(EVENT_CODE));
    }

    // TEST-EVENT-002: evento inexistente → ResourceNotFoundException.
    @Test
    void findByCode_missingEvent_throwsResourceNotFound() {

        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findByCode(EVENT_CODE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // TEST-EVENT-003: crear evento válido → save() ejecutado.
    @Test
    void create_validEvent_savesEvent() {

        Venue venue = venue(true);
        EventResponse response = response(EventStatus.DRAFT);
        when(eventRepository.existsByEventCode(EVENT_CODE)).thenReturn(false);
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(response);

        EventResponse result = eventService.create(request(FUTURE_DATE));

        assertThat(result).isEqualTo(response);
        verify(eventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(eventCaptor.getValue().getVenue()).isSameAs(venue);
    }

    // TEST-EVENT-004: venue inexistente → error y save() nunca ejecutado (BR-EVENT-002).
    @Test
    void create_missingVenue_throwsResourceNotFoundAndNeverSaves() {
        when(eventRepository.existsByEventCode(EVENT_CODE)).thenReturn(false);
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.create(request(FUTURE_DATE)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // TEST-EVENT-005: venue inactivo → BusinessRuleException (BR-EVENT-003).
    @Test
    void create_inactiveVenue_throwsBusinessRule() {
        when(eventRepository.existsByEventCode(EVENT_CODE)).thenReturn(false);
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue(false)));

        assertThatThrownBy(() -> eventService.create(request(FUTURE_DATE)))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // TEST-EVENT-006: fecha pasada → BusinessRuleException (BR-EVENT-004).
    @Test
    void create_pastDate_throwsBusinessRule() {
        when(eventRepository.existsByEventCode(EVENT_CODE)).thenReturn(false);
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue(true)));

        assertThatThrownBy(() -> eventService.create(request(PAST_DATE)))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // TEST-EVENT-007: publicar DRAFT válido → PUBLISHED (BR-EVENT-007 a 009).
    @Test
    void publish_validDraftEvent_becomesPublished() {
        Event event = event(EventStatus.DRAFT, venue(true));
        EventResponse response = response(EventStatus.PUBLISHED);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(response);

        EventResponse result = eventService.publish(EVENT_CODE);

        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository).save(event);
    }

    // TEST-EVENT-008: publicar CANCELLED → BusinessRuleException y no persistir (BR-EVENT-007).
    @Test
    void publish_cancelledEvent_throwsBusinessRuleAndNeverSaves() {
        Event event = event(EventStatus.CANCELLED, venue(true));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.publish(EVENT_CODE))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // FR-SVC-007 (sección 49: unit test requerido): asociar un artista válido.
    @Test
    void addArtist_validArtist_associatesAndSaves() {
        Event event = event(EventStatus.DRAFT, venue(true));
        Artist artist = artist();
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(response(EventStatus.DRAFT));

        eventService.addArtist(EVENT_CODE, ARTIST_ID);

        assertThat(event.getArtists()).containsExactly(artist);
        verify(eventRepository).save(event);
    }

    // BR-EVENT-010: no se asocia dos veces el mismo artista al evento.
    @Test
    void addArtist_artistAlreadyAssociated_throwsDuplicateAndNeverSaves() {
        Event event = event(EventStatus.DRAFT, venue(true));
        Artist artist = artist();
        event.getArtists().add(artist);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist));

        assertThatThrownBy(() -> eventService.addArtist(EVENT_CODE, ARTIST_ID))
                .isInstanceOf(DuplicateResourceException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

       // BR-EVENT-011: no se agregan artistas a eventos CANCELLED.
    @Test
    void addArtist_cancelledEvent_throwsBusinessRuleAndNeverSaves() {
        // ARRANGE
        Event event = event(EventStatus.CANCELLED, venue(true));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist()));

        // ACT & ASSERT
        assertThatThrownBy(() -> eventService.addArtist(EVENT_CODE, ARTIST_ID))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // BR-EVENT-011: no se agregan artistas a eventos FINISHED.
    @Test
    void addArtist_finishedEvent_throwsBusinessRuleAndNeverSaves() {
        // ARRANGE
        Event event = event(EventStatus.FINISHED, venue(true));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(ARTIST_ID)).thenReturn(Optional.of(artist()));

        // ACT & ASSERT
        assertThatThrownBy(() -> eventService.addArtist(EVENT_CODE, ARTIST_ID))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }


}
package edu.unimagdalena.PulsePass.repository;

import edu.unimagdalena.PulsePass.TestcontainersConfiguration;
import edu.unimagdalena.PulsePass.domain.Event;
import edu.unimagdalena.PulsePass.domain.EventCategory;
import edu.unimagdalena.PulsePass.domain.EventStatus;
import edu.unimagdalena.PulsePass.domain.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.LocalDateTime;
import java.util.List;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
// QT-003 (seccion 17): prueba la relacion Venue 1:N Event.
// QT-007: prueba Query Methods simples y con navegacion de relaciones.
// QT-009: prueba una restriccion UNIQUE usando saveAndFlush.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventRepositoryIT {
 
    @Autowired
    private EventRepository eventRepository;
 
    @Autowired
    private VenueRepository venueRepository;
 
    private Venue crearVenue(String code) {
        return venueRepository.saveAndFlush(Venue.builder()
                .code(code)
                .name("Recinto " + code)
                .city("Santa Marta")
                .capacity(1000)
                .active(true)
                .build());
    }
 
    // FR-EVT-001/FR-EVT-002: registrar evento asociado a un venue existente
    // y recuperarlo por eventCode (BR-001, AC-002).
    @Test
    void deberiaGuardarYRecuperarUnEventoPorEventCode() {
        Venue venue = crearVenue("VEN-EVT-01");
 
        Event evento = Event.builder()
                .eventCode("CMF-2026")
                .name("Caribbean Music Fest 2026")
                .description("Festival de musica caribeña")
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 6, 20, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .build();
 
        eventRepository.save(evento);
 
        Event encontrado = eventRepository.findByEventCode("CMF-2026").orElseThrow();
 
        assertThat(encontrado.getName()).isEqualTo("Caribbean Music Fest 2026");
        assertThat(encontrado.getVenue().getCode()).isEqualTo("VEN-EVT-01");
    }
 
    // FR-EVT-005/AC-006: solo se retornan eventos PUBLISHED, en orden
    // cronologico ascendente, sin DRAFT ni CANCELLED.
    @Test
    void deberiaListarSoloEventosPublicadosOrdenadosPorFecha() {
        Venue venue = crearVenue("VEN-EVT-02");
 
        Event publicadoTardio = eventRepository.save(Event.builder()
                .eventCode("EVT-PUB-02")
                .name("Evento publicado tardio")
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 12, 1, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .build());
 
        Event publicadoTemprano = eventRepository.save(Event.builder()
                .eventCode("EVT-PUB-01")
                .name("Evento publicado temprano")
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 3, 1, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .build());
 
        eventRepository.save(Event.builder()
                .eventCode("EVT-DRAFT-01")
                .name("Evento en borrador")
                .category(EventCategory.MUSIC)
                .status(EventStatus.DRAFT)
                .eventDate(LocalDateTime.of(2026, 5, 1, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .build());
 
        eventRepository.save(Event.builder()
                .eventCode("EVT-CANC-01")
                .name("Evento cancelado")
                .category(EventCategory.MUSIC)
                .status(EventStatus.CANCELLED)
                .eventDate(LocalDateTime.of(2026, 4, 1, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .build());
 
        List<Event> publicados = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);
 
        assertThat(publicados)
                .extracting(event -> event.getEventCode())
                .containsExactly(publicadoTemprano.getEventCode(), publicadoTardio.getEventCode());
    }
 
    // FR-VEN-004: recuperar eventos asociados a un venue usando su codigo
    // de negocio (Query Method navegando la relacion Venue 1:N Event).
    @Test
    void deberiaEncontrarEventosPorCodigoDeVenue() {
        Venue venueA = crearVenue("VEN-EVT-03");
        Venue venueB = crearVenue("VEN-EVT-04");
 
        eventRepository.save(Event.builder()
                .eventCode("EVT-VEN-A-01")
                .name("Evento venue A")
                .category(EventCategory.CULTURE)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 7, 1, 19, 0))
                .minimumAge(0)
                .venue(venueA)
                .build());
 
        eventRepository.save(Event.builder()
                .eventCode("EVT-VEN-B-01")
                .name("Evento venue B")
                .category(EventCategory.SPORTS)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 8, 1, 19, 0))
                .minimumAge(0)
                .venue(venueB)
                .build());
 
        List<Event> eventosDeVenueA = eventRepository.findByVenue_Code("VEN-EVT-03");
 
        assertThat(eventosDeVenueA)
                .extracting(event -> event.getEventCode())
                .containsExactly("EVT-VEN-A-01");
    }
 
    // FR-EVT-002/AC-005 (aplicado a Event): PostgreSQL rechaza eventCode
    // duplicado.
    @Test
    void noDeberiaPermitirDosEventosConElMismoEventCode() {
        Venue venue = crearVenue("VEN-EVT-05");
 
        eventRepository.saveAndFlush(Event.builder()
                .eventCode("EVT-DUP-01")
                .name("Primer evento")
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 9, 1, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .build());
 
        Event duplicado = Event.builder()
                .eventCode("EVT-DUP-01")
                .name("Segundo evento")
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 10, 1, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .build();
 
        assertThatThrownBy(() -> eventRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
 
    // BR-001: todo Event debe pertenecer a exactamente un Venue -> la FK
    // venue_id es NOT NULL y la base de datos rechaza un evento sin venue.
    @Test
    void noDeberiaPermitirUnEventoSinVenue() {
        Event eventoSinVenue = Event.builder()
                .eventCode("EVT-SIN-VENUE")
                .name("Evento sin venue")
                .category(EventCategory.MUSIC)
                .status(EventStatus.DRAFT)
                .eventDate(LocalDateTime.of(2026, 11, 1, 20, 0))
                .minimumAge(0)
                .build();
 
        assertThatThrownBy(() -> eventRepository.saveAndFlush(eventoSinVenue))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
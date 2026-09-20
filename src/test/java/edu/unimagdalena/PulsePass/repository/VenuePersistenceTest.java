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

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class VenuePersistenceTest {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void deberiaGuardarYRecuperarUnVenuePorId() {
        Venue venue = Venue.builder()
                .code("VEN-SMR-01")
                .name("Marina Convention Center")
                .city("Santa Marta")
                .capacity(5000)
                .active(true)
                .build();

        Venue guardado = venueRepository.save(venue);

        assertThat(guardado.getId()).isNotNull();

        Venue encontrado = venueRepository.findById(guardado.getId()).orElseThrow();
        assertThat(encontrado.getCode()).isEqualTo("VEN-SMR-01");
        assertThat(encontrado.getCity()).isEqualTo("Santa Marta");
    }

    @Test
    void deberiaEncontrarUnVenuePorSuCode() {
        Venue venue = Venue.builder()
                .code("VEN-BOG-01")
                .name("Movistar Arena")
                .city("Bogota")
                .capacity(15000)
                .active(true)
                .build();
        venueRepository.save(venue);

        Venue encontrado = venueRepository.findByCode("VEN-BOG-01").orElseThrow();

        assertThat(encontrado.getName()).isEqualTo("Movistar Arena");
    }

    @Test
    void unVenuePuedeTenerVariosEventosAsociados() {
        Venue venue = Venue.builder()
                .code("VEN-CTG-01")
                .name("Centro de Convenciones Cartagena de Indias")
                .city("Cartagena")
                .capacity(3000)
                .active(true)
                .build();
        venueRepository.save(venue);

        Event evento1 = Event.builder()
                .eventCode("EVT-001")
                .name("Festival de Jazz")
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 11, 10, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .build();

        Event evento2 = Event.builder()
                .eventCode("EVT-002")
                .name("Conferencia de Tecnologia")
                .category(EventCategory.TECHNOLOGY)
                .status(EventStatus.DRAFT)
                .eventDate(LocalDateTime.of(2026, 12, 5, 9, 0))
                .minimumAge(0)
                .venue(venue)
                .build();

        eventRepository.save(evento1);
        eventRepository.save(evento2);

        List<Event> eventosDelVenue = eventRepository.findByVenue_Code("VEN-CTG-01");

        assertThat(eventosDelVenue).hasSize(2);
        assertThat(eventosDelVenue)
                .extracting(event -> event.getEventCode())
                .containsExactlyInAnyOrder("EVT-001", "EVT-002");
    }

    @Test
    void noDeberiaPermitirDosVenuesConElMismoCode() {
        Venue venue1 = Venue.builder()
                .code("VEN-DUP-01")
                .name("Recinto Uno")
                .city("Medellin")
                .capacity(2000)
                .active(true)
                .build();
        venueRepository.saveAndFlush(venue1);

        Venue venue2 = Venue.builder()
                .code("VEN-DUP-01")
                .name("Recinto Dos")
                .city("Cali")
                .capacity(1000)
                .active(true)
                .build();

        assertThatThrownBy(() -> venueRepository.saveAndFlush(venue2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void noDeberiaPermitirUnVenueConCapacidadNoPositiva() {
        Venue venueInvalido = Venue.builder()
                .code("VEN-INV-01")
                .name("Recinto Invalido")
                .city("Barranquilla")
                .capacity(0)
                .active(true)
                .build();

        assertThatThrownBy(() -> venueRepository.saveAndFlush(venueInvalido))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

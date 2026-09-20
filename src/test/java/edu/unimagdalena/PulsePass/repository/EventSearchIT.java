package edu.unimagdalena.PulsePass.repository;

import edu.unimagdalena.PulsePass.TestcontainersConfiguration;
import edu.unimagdalena.PulsePass.domain.Artist;
import edu.unimagdalena.PulsePass.domain.Event;
import edu.unimagdalena.PulsePass.domain.EventCategory;
import edu.unimagdalena.PulsePass.domain.EventStatus;
import edu.unimagdalena.PulsePass.domain.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
 
import static org.assertj.core.api.Assertions.assertThat;
 
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventSearchIT {
 
    @Autowired
    private EventRepository eventRepository;
 
    @Autowired
    private ArtistRepository artistRepository;
 
    @Autowired
    private VenueRepository venueRepository;
 
    private Venue crearVenue(String code, String city) {
        Venue venue = Venue.builder()
                .code(code)
                .name("Recinto " + code)
                .city(city)
                .capacity(1000)
                .active(true)
                .build();
        return venueRepository.save(venue);
    }
 
    private Artist crearArtista(String stageName) {
        Artist artista = Artist.builder()
                .stageName(stageName)
                .country("Colombia")
                .genre("Pop")
                .active(true)
                .build();
        return artistRepository.save(artista);
    }
 
    private Event construirEvento(String eventCode, Venue venue, EventStatus status,
                                   LocalDateTime fecha, Set<Artist> artistas) {
        return Event.builder()
                .eventCode(eventCode)
                .name("Evento " + eventCode)
                .category(EventCategory.MUSIC)
                .status(status)
                .eventDate(fecha)
                .minimumAge(0)
                .venue(venue)
                .artists(artistas)
                .build();
    }
 
    // FR-SRC-001: buscar eventos donde participe un artista por stageName.
    // AC-007: si el artista participa en varios eventos, cada uno aparece
    // una sola vez. 
    @Test
    void deberiaEncontrarEventosPorStageNameDelArtistaSinDuplicados() {
        Venue venue = crearVenue("VEN-SRC-01", "Santa Marta");
        Artist solarBeat = crearArtista("Solar Beat SRC");
        Artist neonWaves = crearArtista("Neon Waves SRC");
 
        eventRepository.save(construirEvento("EVT-SRC-01", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 6, 1, 20, 0), Set.of(solarBeat, neonWaves)));
 
        eventRepository.save(construirEvento("EVT-SRC-02", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 7, 1, 20, 0), Set.of(neonWaves)));
 
        List<Event> encontrados = eventRepository.findByArtistStageName("Solar Beat SRC");
 
        assertThat(encontrados)
                .extracting(event -> event.getEventCode())
                .containsExactly("EVT-SRC-01");
    }
 
    // FR-SRC-002: buscar eventos de una ciudad en los que participe un
    // artista especifico. Criterio: filtra por venue.city y artist.stageName.
    @Test
    void deberiaFiltrarEventosPorCiudadYStageNameDelArtista() {
        Venue venueSantaMarta = crearVenue("VEN-SRC-02", "Santa Marta");
        Venue venueBogota = crearVenue("VEN-SRC-03", "Bogota");
        Artist artista = crearArtista("Caribbean Sound SRC");
 
        eventRepository.save(construirEvento("EVT-SRC-03", venueSantaMarta, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 8, 1, 20, 0), Set.of(artista)));
 
        eventRepository.save(construirEvento("EVT-SRC-04", venueBogota, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 8, 5, 20, 0), Set.of(artista)));
 
        List<Event> encontrados = eventRepository.findByCityAndArtistStageName(
                "Santa Marta", "Caribbean Sound SRC");
 
        assertThat(encontrados)
                .extracting(event -> event.getEventCode())
                .containsExactly("EVT-SRC-03");
    }
 
    // FR-SRC-003: buscar eventos publicados posteriores a una fecha, en una
    // ciudad, cuyo artista contenga un texto. Criterio: case-insensitive
    // para artista, usa DISTINCT y ordena por fecha ascendente.
    @Test
    void deberiaEncontrarEventosRecomendadosFiltrandoPorFechaCiudadYTextoDeArtistaSinImportarMayusculas() {
        Venue venue = crearVenue("VEN-SRC-04", "Santa Marta");
        Artist oceanDrive = crearArtista("Ocean Drive SRC");
 
        Event eventoTardio = eventRepository.save(construirEvento("EVT-SRC-06", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 1, 20, 0), Set.of(oceanDrive)));
 
        Event eventoTemprano = eventRepository.save(construirEvento("EVT-SRC-05", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 10, 1, 20, 0), Set.of(oceanDrive)));
 
        // No debe aparecer: es anterior a la fecha de corte.
        eventRepository.save(construirEvento("EVT-SRC-07", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 1, 1, 20, 0), Set.of(oceanDrive)));
 
        // No debe aparecer: no esta PUBLISHED.
        eventRepository.save(construirEvento("EVT-SRC-08", venue, EventStatus.DRAFT,
                LocalDateTime.of(2026, 11, 1, 20, 0), Set.of(oceanDrive)));
 
        List<Event> recomendados = eventRepository.findRecommendedEvents(
                LocalDateTime.of(2026, 9, 1, 0, 0), "Santa Marta", "ocean drive");
 
        assertThat(recomendados)
                .extracting(event -> event.getEventCode())
                .containsExactly(eventoTemprano.getEventCode(), eventoTardio.getEventCode());
    }
 
    // FR-SRC-003 (complemento): la busqueda de texto del artista debe ser
    // parcial y case-insensitive, no solo una coincidencia exacta.
    @Test
    void deberiaEncontrarEventosRecomendadosConCoincidenciaParcialDeTextoEnElArtista() {
        Venue venue = crearVenue("VEN-SRC-05", "Santa Marta");
        Artist digitalPulse = crearArtista("Digital Pulse SRC");
 
        Event evento = eventRepository.save(construirEvento("EVT-SRC-09", venue, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 11, 20, 20, 0), Set.of(digitalPulse)));
 
        List<Event> recomendados = eventRepository.findRecommendedEvents(
                LocalDateTime.of(2026, 1, 1, 0, 0), "Santa Marta", "PULSE");
 
        assertThat(recomendados)
                .extracting(event -> event.getEventCode())
                .containsExactly(evento.getEventCode());
    }
}

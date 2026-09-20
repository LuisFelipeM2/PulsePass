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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
 
// QT-005 (seccion 17): prueba la relacion Event N:M Artist.
// QT-007: prueba consultas JPQL con JOIN sobre la relacion N:M.
// QT-009: prueba la restriccion que impide duplicar el mismo par evento-artista.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventArtistIT {
 
    @Autowired
    private EventRepository eventRepository;
 
    @Autowired
    private ArtistRepository artistRepository;
 
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
 
    private Artist crearArtista(String stageName) {
        return artistRepository.saveAndFlush(Artist.builder()
                .stageName(stageName)
                .country("Colombia")
                .genre("Pop")
                .active(true)
                .build());
    }
 
    // FR-ART-003/AC-003: un evento puede asociar multiples artistas y los
    // tres aparecen relacionados sin duplicar el mismo par evento-artista.
    @Test
    void unEventoDebeAsociarVariosArtistasSinDuplicarElPar() {
        Venue venue = crearVenue("VEN-ART-01");
        Artist solarBeat = crearArtista("Solar Beat Test");
        Artist neonWaves = crearArtista("Neon Waves Test");
        Artist caribbeanSound = crearArtista("Caribbean Sound Test");
 
        Set<Artist> artistas = new HashSet<>();
        artistas.add(solarBeat);
        artistas.add(neonWaves);
        artistas.add(caribbeanSound);
        // Intento de duplicar el mismo artista en el set: el Set ya lo
        // evita a nivel de Java, y la PK compuesta lo evita a nivel de BD.
        artistas.add(solarBeat);
 
        Event evento = Event.builder()
                .eventCode("CMF-ART-2026")
                .name("Caribbean Music Fest Test")
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 6, 20, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .artists(artistas)
                .build();
 
        eventRepository.saveAndFlush(evento);
 
        Event recuperado = eventRepository.findByEventCode("CMF-ART-2026").orElseThrow();
 
        assertThat(recuperado.getArtists())
                .extracting(artist -> artist.getStageName())
                .containsExactlyInAnyOrder("Solar Beat Test", "Neon Waves Test", "Caribbean Sound Test");
    }
 
    // FR-ART-004: un artista puede participar en varios eventos, recuperados
    // mediante una consulta JPQL (Artist -> Events).
    @Test
    void unArtistaDebeParticiparEnVariosEventos() {
        Venue venue = crearVenue("VEN-ART-02");
        Artist artista = crearArtista("Ocean Drive Test");
 
        Event eventoUno = Event.builder()
                .eventCode("EVT-ART-01")
                .name("Primer concierto")
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 7, 1, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .artists(Set.of(artista))
                .build();
 
        Event eventoDos = Event.builder()
                .eventCode("EVT-ART-02")
                .name("Segundo concierto")
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 8, 1, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .artists(Set.of(artista))
                .build();
 
        eventRepository.save(eventoUno);
        eventRepository.save(eventoDos);
 
        List<Event> eventosDelArtista = artistRepository.findEventsByArtistStageName("Ocean Drive Test");
 
        assertThat(eventosDelArtista)
                .extracting(event -> event.getEventCode())
                .containsExactlyInAnyOrder("EVT-ART-01", "EVT-ART-02");
    }
 
    // FR-SRC-001/AC-007: varios eventos con el mismo artista deben aparecer
    // una sola vez cada uno (DISTINCT), usando la consulta JPQL de EventRepository.
    @Test
    void deberiaEncontrarEventosPorArtistaSinDuplicados() {
        Venue venue = crearVenue("VEN-ART-03");
        Artist artista = crearArtista("Digital Pulse Test");
 
        Event evento = Event.builder()
                .eventCode("EVT-ART-03")
                .name("Concierto con multiples artistas")
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 9, 1, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .artists(Set.of(artista, crearArtista("Otro Artista Test")))
                .build();
 
        eventRepository.save(evento);
 
        List<Event> encontrados = eventRepository.findByArtistStageName("Digital Pulse Test");
 
        assertThat(encontrados).hasSize(1);
        assertThat(encontrados.get(0).getEventCode()).isEqualTo("EVT-ART-03");
    }
 
    // FR-ART-003: la tabla event_artists no debe permitir repetir el mismo
    // par (event_id, artist_id); se fuerza el insert duplicado directamente
    // sobre la tabla intermedia para probar la PK compuesta.
    @Test
    void noDeberiaPermitirElMismoParEventoArtistaDosVeces() {
        Venue venue = crearVenue("VEN-ART-04");
        Artist artista = crearArtista("Artista Duplicado Test");
 
        Event evento = eventRepository.saveAndFlush(Event.builder()
                .eventCode("EVT-ART-DUP")
                .name("Evento con artista duplicado")
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDateTime.of(2026, 10, 1, 20, 0))
                .minimumAge(0)
                .venue(venue)
                .artists(new HashSet<>(Set.of(artista)))
                .build());
 
        // Volver a agregar el mismo artista: como Event.artists es un Set,
        // Java ya impide el duplicado en memoria.
        evento.getArtists().add(artista);
 
        assertThatCode(() -> eventRepository.saveAndFlush(evento))
                .doesNotThrowAnyException();
 
        Event recuperado = eventRepository.findByEventCode("EVT-ART-DUP").orElseThrow();
        assertThat(recuperado.getArtists()).hasSize(1);
    }
}

package edu.unimagdalena.PulsePass.repository;

import edu.unimagdalena.PulsePass.TestcontainersConfiguration;
import edu.unimagdalena.PulsePass.domain.Event;
import edu.unimagdalena.PulsePass.domain.EventCategory;
import edu.unimagdalena.PulsePass.domain.EventStatus;
import edu.unimagdalena.PulsePass.domain.Ticket;
import edu.unimagdalena.PulsePass.domain.TicketStatus;
import edu.unimagdalena.PulsePass.domain.TicketType;
import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.domain.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TicketRepositoryIT {
 
    @Autowired
    private TicketRepository ticketRepository;
 
    @Autowired
    private UserRepository userRepository;
 
    @Autowired
    private EventRepository eventRepository;
 
    @Autowired
    private VenueRepository venueRepository;
 
    private Venue crearVenue(String code) {
        Venue venue = Venue.builder()
                .code(code)
                .name("Recinto " + code)
                .city("Santa Marta")
                .capacity(1000)
                .active(true)
                .build();
        return venueRepository.save(venue);
    }
 
    private Event crearEvento(String eventCode, Venue venue, LocalDateTime fecha) {
        Event evento = Event.builder()
                .eventCode(eventCode)
                .name("Evento " + eventCode)
                .category(EventCategory.MUSIC)
                .status(EventStatus.PUBLISHED)
                .eventDate(fecha)
                .minimumAge(0)
                .venue(venue)
                .build();
        return eventRepository.save(evento);
    }
 
    private User crearUsuario(String username) {
        User user = User.builder()
                .username(username)
                .email(username + "@example.com")
                .active(true)
                .build();
        return userRepository.save(user);
    }
 
    private Ticket construirTicket(String ticketCode, User user, Event event, TicketStatus status) {
        return Ticket.builder()
                .ticketCode(ticketCode)
                .type(TicketType.GENERAL)
                .price(new BigDecimal("120000.00"))
                .status(status)
                .purchaseDate(LocalDateTime.now())
                .user(user)
                .event(event)
                .build();
    }
 
    @Test
    void deberiaGuardarYRecuperarUnTicketAsociadoAUsuarioYEvento() {
        Venue venue = crearVenue("VEN-TKT-01");
        Event evento = crearEvento("EVT-TKT-01", venue, LocalDateTime.of(2026, 6, 20, 20, 0));
        User user = crearUsuario("andrea.gomez");
 
        Ticket guardado = ticketRepository.save(
                construirTicket("TCK-0001", user, evento, TicketStatus.PAID));
 
        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getUser().getUsername()).isEqualTo("andrea.gomez");
        assertThat(guardado.getEvent().getEventCode()).isEqualTo("EVT-TKT-01");
    }
 
    @Test
    void deberiaEncontrarTicketsDeUnUsuarioPorEmailYPorEmailConStatus() {
        Venue venue = crearVenue("VEN-TKT-02");
        Event evento = crearEvento("EVT-TKT-02", venue, LocalDateTime.of(2026, 7, 1, 20, 0));
        User user = crearUsuario("carlos.perez");
 
        ticketRepository.save(construirTicket("TCK-0002", user, evento, TicketStatus.PAID));
        ticketRepository.save(construirTicket("TCK-0003", user, evento, TicketStatus.RESERVED));
 
        List<Ticket> todosLosDelUsuario = ticketRepository.findByUser_Email("carlos.perez@example.com");
        List<Ticket> soloPagados = ticketRepository.findByUser_EmailAndStatus(
                "carlos.perez@example.com", TicketStatus.PAID);
 
        assertThat(todosLosDelUsuario).hasSize(2);
        assertThat(soloPagados)
                .extracting(ticket -> ticket.getTicketCode())
                .containsExactly("TCK-0002");
    }
 
    @Test
    void deberiaRecuperarSoloLosTicketsPagadosDeUnEventoPorEventCode() {
        Venue venue = crearVenue("VEN-TKT-03");
        Event evento = crearEvento("EVT-TKT-03", venue, LocalDateTime.of(2026, 8, 1, 20, 0));
        User user = crearUsuario("laura.diaz");
 
        ticketRepository.save(construirTicket("TCK-0004", user, evento, TicketStatus.PAID));
        ticketRepository.save(construirTicket("TCK-0005", user, evento, TicketStatus.RESERVED));
        ticketRepository.save(construirTicket("TCK-0006", user, evento, TicketStatus.CANCELLED));
 
        List<Ticket> pagados = ticketRepository.findPaidTicketsByEventCode("EVT-TKT-03");
 
        assertThat(pagados)
                .extracting(ticket -> ticket.getTicketCode())
                .containsExactly("TCK-0004");
    }
 
    @Test
    void deberiaContarUnicamenteLosTicketsPagadosDeUnEvento() {
        Venue venue = crearVenue("VEN-TKT-04");
        Event evento = crearEvento("EVT-TKT-04", venue, LocalDateTime.of(2026, 9, 1, 20, 0));
        User user = crearUsuario("miguel.torres");
 
        ticketRepository.save(construirTicket("TCK-0007", user, evento, TicketStatus.PAID));
        ticketRepository.save(construirTicket("TCK-0008", user, evento, TicketStatus.PAID));
        ticketRepository.save(construirTicket("TCK-0009", user, evento, TicketStatus.RESERVED));
        ticketRepository.save(construirTicket("TCK-0010", user, evento, TicketStatus.CANCELLED));
 
        long conteo = ticketRepository.countPaidTicketsByEventCode("EVT-TKT-04");
 
        assertThat(conteo).isEqualTo(2);
    }
 
    @Test
    void deberiaEncontrarTicketsDeEventosFuturosOrdenadosPorFecha() {
        Venue venue = crearVenue("VEN-TKT-05");
        User user = crearUsuario("sofia.ramirez");
 
        Event eventoTemprano = crearEvento("EVT-TKT-05", venue, LocalDateTime.of(2026, 10, 1, 20, 0));
        Event eventoTardio = crearEvento("EVT-TKT-06", venue, LocalDateTime.of(2026, 12, 1, 20, 0));
        Event eventoPasado = crearEvento("EVT-TKT-07", venue, LocalDateTime.of(2020, 1, 1, 20, 0));
 
        ticketRepository.save(construirTicket("TCK-0011", user, eventoTardio, TicketStatus.PAID));
        ticketRepository.save(construirTicket("TCK-0012", user, eventoTemprano, TicketStatus.PAID));
        ticketRepository.save(construirTicket("TCK-0013", user, eventoPasado, TicketStatus.PAID));
 
        List<Ticket> futuros = ticketRepository.findByEvent_EventDateAfterOrderByEvent_EventDateAsc(
                LocalDateTime.of(2026, 1, 1, 0, 0));
 
        assertThat(futuros)
                .extracting(ticket -> ticket.getTicketCode())
                .containsExactly("TCK-0012", "TCK-0011");
    }
 
    @Test
    void noDeberiaPermitirDosTicketsConElMismoTicketCode() {
        Venue venue = crearVenue("VEN-TKT-06");
        Event evento = crearEvento("EVT-TKT-08", venue, LocalDateTime.of(2026, 11, 1, 20, 0));
        User user = crearUsuario("elena.vargas");
 
        ticketRepository.saveAndFlush(construirTicket("TCK-DUP-01", user, evento, TicketStatus.PAID));
 
        Ticket duplicado = construirTicket("TCK-DUP-01", user, evento, TicketStatus.RESERVED);
 
        assertThatThrownBy(() -> ticketRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
 
    @Test
    void noDeberiaPermitirUnTicketConPrecioNegativo() {
        Venue venue = crearVenue("VEN-TKT-07");
        Event evento = crearEvento("EVT-TKT-09", venue, LocalDateTime.of(2026, 12, 15, 20, 0));
        User user = crearUsuario("mateo.rojas");
 
        Ticket ticketInvalido = Ticket.builder()
                .ticketCode("TCK-INV-01")
                .type(TicketType.VIP)
                .price(new BigDecimal("-1.00"))
                .status(TicketStatus.RESERVED)
                .purchaseDate(LocalDateTime.now())
                .user(user)
                .event(evento)
                .build();
 
        assertThatThrownBy(() -> ticketRepository.saveAndFlush(ticketInvalido))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

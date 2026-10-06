package edu.unimagdalena.PulsePass.service;

import edu.unimagdalena.PulsePass.domain.Event;
import edu.unimagdalena.PulsePass.domain.EventStatus;
import edu.unimagdalena.PulsePass.domain.Ticket;
import edu.unimagdalena.PulsePass.domain.TicketStatus;
import edu.unimagdalena.PulsePass.domain.TicketType;
import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.domain.UserProfile;
import edu.unimagdalena.PulsePass.domain.Venue;
import edu.unimagdalena.PulsePass.dto.request.PurchaseTicketRequest;
import edu.unimagdalena.PulsePass.dto.response.TicketResponse;
import edu.unimagdalena.PulsePass.exception.BusinessRuleException;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.mapper.TicketMapper;
import edu.unimagdalena.PulsePass.repository.EventRepository;
import edu.unimagdalena.PulsePass.repository.TicketRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;
import edu.unimagdalena.PulsePass.service.impl.TicketServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final String EMAIL = "andrea@email.com";
    private static final String EVENT_CODE = "CMF-2026";
    private static final String TICKET_CODE = "TKT-TEST-001";
    private static final int CAPACITY = 3;                      
    private static final LocalDateTime EVENT_DATE = LocalDateTime.now().plusDays(30);

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketMapper ticketMapper;
    @Captor
    private ArgumentCaptor<Ticket> ticketCaptor;

    @InjectMocks
    private TicketServiceImpl ticketService;

    // Datos de prueba 
    private void stubValidPurchase(User user, Event event, long paidTickets) {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID))
                .thenReturn(paidTickets);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(response());
    }

    private User user(boolean active, int ageAtEvent) {
        return User.builder()
                .id(1L)
                .username("andrea")
                .email(EMAIL)
                .active(active)
                .profile(UserProfile.builder()
                        .birthDate(EVENT_DATE.toLocalDate().minusYears(ageAtEvent))
                        .build())
                .build();
    }

    private Event event(EventStatus status) {
        Venue venue = Venue.builder()
                .id(1L)
                .code("VEN-SMR-01")
                .capacity(CAPACITY)
                .active(true)
                .build();
        return Event.builder()
                .id(1L)
                .eventCode(EVENT_CODE)
                .name("Caribbean Music Fest 2026")
                .status(status)
                .eventDate(EVENT_DATE)
                .minimumAge(18)
                .venue(venue)
                .build();
    }

    private Ticket ticket(TicketStatus status) {
        return Ticket.builder()
                .id(1L)
                .ticketCode(TICKET_CODE)
                .type(TicketType.GENERAL)
                .price(new BigDecimal("100000.00"))
                .status(status)
                .purchaseDate(LocalDateTime.now())
                .user(user(true, 25))
                .event(event(EventStatus.PUBLISHED))
                .build();
    }

    private PurchaseTicketRequest request(TicketType type) {
        return new PurchaseTicketRequest(EMAIL, EVENT_CODE, type);
    }

    private TicketResponse response() {
        return new TicketResponse(1L, TICKET_CODE, TicketType.GENERAL, new BigDecimal("100000.00"),
                TicketStatus.PAID, LocalDateTime.now(), EMAIL, EVENT_CODE, "Caribbean Music Fest 2026");
    }

    // TEST-TICKET-001: compra válida → ticket PAID.
    @Test
    void purchase_validRequest_createsPaidTicket() {
        User user = user(true, 25);
        Event event = event(EventStatus.PUBLISHED);
        stubValidPurchase(user, event, 0L);

        ticketService.purchase(request(TicketType.GENERAL));

        verify(ticketRepository).save(ticketCaptor.capture());
        Ticket saved = ticketCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getEvent()).isSameAs(event);
        assertThat(saved.getTicketCode()).startsWith("TKT-");
        assertThat(saved.getPurchaseDate()).isNotNull();
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // TEST-TICKET-002: usuario inexistente → ResourceNotFoundException (BR-TICKET-001).
    @Test
    void purchase_missingUser_throwsResourceNotFound() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // TEST-TICKET-003: usuario inactivo → BusinessRuleException (BR-TICKET-002).
    @Test
    void purchase_inactiveUser_throwsBusinessRule() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user(false, 30)));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // TEST-TICKET-004: evento DRAFT → BusinessRuleException (BR-TICKET-004).
    @Test
    void purchase_draftEvent_throwsBusinessRule() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user(true, 25)));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event(EventStatus.DRAFT)));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // TEST-TICKET-005: evento CANCELLED → BusinessRuleException (BR-TICKET-004).
    @Test
    void purchase_cancelledEvent_throwsBusinessRule() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user(true, 25)));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event(EventStatus.CANCELLED)));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // TEST-TICKET-006: usuario menor de edad → BusinessRuleException (BR-TICKET-006).
    // Laura tiene 17 años en la fecha del evento y el evento exige 18 (sección 47).
    @Test
    void purchase_underageUser_throwsBusinessRule() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user(true, 17)));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event(EventStatus.PUBLISHED)));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("User does not meet minimum age.");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // TEST-TICKET-007: evento sin capacidad → BusinessRuleException (BR-TICKET-007).
    @Test
    void purchase_noCapacityLeft_throwsBusinessRule() {

        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user(true, 25)));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event(EventStatus.PUBLISHED)));
        when(ticketRepository.countByEventEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID))
                .thenReturn((long) CAPACITY);

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // TEST-TICKET-008: último ticket disponible → guarda el ticket y el evento pasa a SOLD_OUT
    // (BR-TICKET-008).
    @Test
    void purchase_lastAvailableTicket_savesTicketAndSetsSoldOut() {
        Event event = event(EventStatus.PUBLISHED);
        stubValidPurchase(user(true, 25), event, CAPACITY - 1L);

        ticketService.purchase(request(TicketType.GENERAL));

        verify(ticketRepository).save(any(Ticket.class));
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        verify(eventRepository).save(event);
    }

    // TEST-TICKET-009: cancelar ticket PAID → CANCELLED (BR-TICKET-010).
    @Test
    void cancel_paidTicket_becomesCancelled() {
        Ticket ticket = ticket(TicketStatus.PAID);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(response());

        ticketService.cancel(TICKET_CODE);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository).save(ticket);
    }

    // TEST-TICKET-010: cancelar ticket USED → BusinessRuleException (BR-TICKET-011).
    @Test
    void cancel_usedTicket_throwsBusinessRule() {
        // ARRANGE
        when(ticketRepository.findByTicketCode(TICKET_CODE))
                .thenReturn(Optional.of(ticket(TicketStatus.USED)));

        // ACT & ASSERT
        assertThatThrownBy(() -> ticketService.cancel(TICKET_CODE))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // TEST-TICKET-011: marcar ticket PAID como usado → USED (BR-TICKET-013).
    @Test
    void markAsUsed_paidTicket_becomesUsed() {
        Ticket ticket = ticket(TicketStatus.PAID);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(response());

        ticketService.markAsUsed(TICKET_CODE);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository).save(ticket);
    }

    // TEST-TICKET-012: usar ticket CANCELLED → BusinessRuleException (BR-TICKET-014).
    @Test
    void markAsUsed_cancelledTicket_throwsBusinessRule() {
        when(ticketRepository.findByTicketCode(TICKET_CODE))
                .thenReturn(Optional.of(ticket(TicketStatus.CANCELLED)));

        assertThatThrownBy(() -> ticketService.markAsUsed(TICKET_CODE))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

}
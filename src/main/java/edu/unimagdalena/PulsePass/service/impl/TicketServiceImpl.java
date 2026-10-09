package edu.unimagdalena.PulsePass.service.impl;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.unimagdalena.PulsePass.domain.Event;
import edu.unimagdalena.PulsePass.domain.EventStatus;
import edu.unimagdalena.PulsePass.domain.Ticket;
import edu.unimagdalena.PulsePass.domain.TicketStatus;
import edu.unimagdalena.PulsePass.domain.TicketType;
import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.dto.request.PurchaseTicketRequest;
import edu.unimagdalena.PulsePass.dto.response.TicketResponse;
import edu.unimagdalena.PulsePass.exception.BusinessRuleException;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.mapper.TicketMapper;
import edu.unimagdalena.PulsePass.repository.EventRepository;
import edu.unimagdalena.PulsePass.repository.TicketRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;
import edu.unimagdalena.PulsePass.service.TicketService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

// SRV-003: implementación de TicketService.
// SRV-004: escrituras con @Transactional, lecturas con readOnly = true.
@Service
public class TicketServiceImpl implements TicketService {

    // Valor de ejemplo: el PRD no define el precio base (sección 28).
    private static final BigDecimal BASE_PRICE = new BigDecimal("100000.00");

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             UserRepository userRepository,
                             EventRepository eventRepository,
                             TicketMapper ticketMapper) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
    }

    // FR-SVC-013: comprar ticket (secciones 24 a 28).
    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {

        LocalDateTime now = LocalDateTime.now();

        // BR-TICKET-001: el usuario debe existir.
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userEmail()));

        // BR-TICKET-002: un usuario inactivo no puede comprar.
        if (!user.getActive()) {
            throw new BusinessRuleException("User is not active: " + user.getEmail());
        }

        // BR-TICKET-003: el evento debe existir.
        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event", request.eventCode()));

        // BR-TICKET-004: solo se compra si el evento está PUBLISHED
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Event is not available for purchase. Current status: " + event.getStatus());
        }

        // BR-TICKET-005: no se compra para un evento que ya ocurrió.
        if (!event.getEventDate().isAfter(now)) {
            throw new BusinessRuleException("Event has already taken place.");
        }

        // BR-TICKET-006: si el evento tiene edad mínima, se calcula la edad del usuario
        // con UserProfile.birthDate, evaluada en la fecha del evento (no en la de hoy).
        if (event.getMinimumAge() > 0) {
            LocalDate birthDate = user.getProfile().getBirthDate();
            int age = Period.between(birthDate, event.getEventDate().toLocalDate()).getYears();
            if (age < event.getMinimumAge()) {
                throw new BusinessRuleException("User does not meet minimum age.");
            }
        }

        // BR-TICKET-007: si paidTickets >= capacity del venue, no se permite otra compra.
        long paidTickets = ticketRepository
                .countByEventEventCodeAndStatus(request.eventCode(), TicketStatus.PAID);
        int capacity = event.getVenue().getCapacity();
        if (paidTickets >= capacity) {
            throw new BusinessRuleException("Event has no capacity left: " + request.eventCode());
        }

        // Sección 28: el precio lo calcula el sistema según el tipo; el cliente no lo envía.
        BigDecimal price = calculatePrice(request.type());

        // BR-TICKET-009: nunca se permite un precio negativo.
        if (price.signum() < 0) {
            throw new BusinessRuleException("Ticket price cannot be negative.");
        }

        Ticket ticket = Ticket.builder()
                .ticketCode("TKT-" + UUID.randomUUID())
                .type(request.type())
                .price(price)
                // Sección 27: una compra válida genera un ticket PAID.
                .status(TicketStatus.PAID)
                .purchaseDate(now)
                .user(user)
                .event(event)
                .build();
        Ticket savedTicket = ticketRepository.save(ticket);

        // BR-TICKET-008: si esta compra llena la capacidad, el evento pasa a SOLD_OUT.
        if (paidTickets + 1 == capacity) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(savedTicket);
    }

    // FR-SVC-014: consultar ticket por código.
    @Override
    @Transactional(readOnly = true)
    public TicketResponse findByCode(String ticketCode) {
        return ticketMapper.toResponse(findTicketOrThrow(ticketCode));
    }

    // FR-SVC-015: consultar tickets de un usuario, del más reciente al más antiguo.
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    // FR-SVC-016: consultar tickets pagados de un evento.
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findPaidTicketsByEventCode(eventCode)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    // FR-SVC-017: cancelar ticket, PAID → CANCELLED (sección 29).
    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = findTicketOrThrow(ticketCode);

        // BR-TICKET-010: solo se cancela un ticket PAID.
        // BR-TICKET-011: por lo tanto, un ticket USED o CANCELLED no se puede cancelar.
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Current status: " + ticket.getStatus());
        }

        // BR-TICKET-012: no se cancela después de la fecha del evento.
        if (LocalDateTime.now().isAfter(ticket.getEvent().getEventDate())) {
            throw new BusinessRuleException("Ticket cannot be cancelled after the event date.");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    // FR-SVC-018: marcar ticket como usado, PAID → USED (sección 30).
    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = findTicketOrThrow(ticketCode);

        // BR-TICKET-013: solo un ticket PAID se marca como usado.
        // BR-TICKET-014: por lo tanto, un ticket CANCELLED nunca puede utilizarse.
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be used. Current status: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.USED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    // Sección 28: la estrategia de precio está encapsulada en este método.
    // GENERAL → precio base, STUDENT → descuento, VIP y BACKSTAGE → multiplicadores.
    // Se usa BigDecimal, nunca float ni double.
    private BigDecimal calculatePrice(TicketType type) {
        BigDecimal price = switch (type) {
            case GENERAL -> BASE_PRICE;
            case STUDENT -> BASE_PRICE.multiply(new BigDecimal("0.80"));
            case VIP -> BASE_PRICE.multiply(new BigDecimal("2.00"));
            case BACKSTAGE -> BASE_PRICE.multiply(new BigDecimal("3.00"));
        };

        // La columna price tiene escala 2.
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    // Sección 36: Optional con orElseThrow, nunca get() sin comprobar.
    private Ticket findTicketOrThrow(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketCode));
    }
}
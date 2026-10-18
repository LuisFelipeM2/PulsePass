package edu.unimagdalena.PulsePass.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unimagdalena.PulsePass.dto.request.PurchaseTicketRequest;
import edu.unimagdalena.PulsePass.dto.response.TicketResponse;
import edu.unimagdalena.PulsePass.security.SecurityPrincipal;
import edu.unimagdalena.PulsePass.service.AuthorizationService;
import edu.unimagdalena.PulsePass.service.TicketService;
import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/api")
public class TicketController {

    private final TicketService ticketService;
    private final AuthorizationService authorizationService;

    public TicketController(TicketService ticketService, AuthorizationService authorizationService) {
        this.ticketService = ticketService;
        this.authorizationService = authorizationService;
    }

    // FR-CTRL-TKT-001: 201 Created; las reglas de compra (edad, capacidad, estado) las aplica el Service -> 409.
    @PostMapping ("/tickets")
    public ResponseEntity<TicketResponse> purchase(@Valid @RequestBody PurchaseTicketRequest request, @AuthenticationPrincipal SecurityPrincipal principal) {
        authorizationService.requirePurchaseForSelf(principal, request.userEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.purchase(request));
    }

    // FR-CTRL-TKT-003: tickets de un usuario por email.
    @GetMapping ("/tickets/by-user")
    public ResponseEntity<List<TicketResponse>> findByUserEmail(@RequestParam String email, @AuthenticationPrincipal SecurityPrincipal principal) {
        authorizationService.requireSelfOrAdminByEmail(principal, email);
        return ResponseEntity.ok(ticketService.findByUserEmail(email));
    }

    // FR-CTRL-TKT-002: 200 o 404.
    @GetMapping("/tickets/{ticketCode}")
    public ResponseEntity<TicketResponse> findByCode(@PathVariable String ticketCode, @AuthenticationPrincipal SecurityPrincipal principal) {
        authorizationService.requireTicketAccess(principal, ticketCode);
        return ResponseEntity.ok(ticketService.findByCode(ticketCode));
    }

    // FR-CTRL-TKT-004: tickets PAID de un evento.
    @GetMapping("/events/{eventCode}/tickets/paid")
    public ResponseEntity<List<TicketResponse>> findPaidTicketsByEvent(@PathVariable String eventCode) {
        return ResponseEntity.ok(ticketService.findPaidTicketsByEvent(eventCode));
    }

    // FR-CTRL-TKT-005: PAID -> CANCELLED; transición inválida -> 409.
    @PatchMapping ("/tickets/{ticketCode}/cancel")
    public ResponseEntity<TicketResponse> cancel(@PathVariable String ticketCode, @AuthenticationPrincipal SecurityPrincipal principal) {
        authorizationService.requireTicketCancel(principal, ticketCode);
        return ResponseEntity.ok(ticketService.cancel(ticketCode));
    }

    @PatchMapping("/tickets/{ticketCode}/use")
    public ResponseEntity<TicketResponse> markAsUsed(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.markAsUsed(ticketCode));
    }
}
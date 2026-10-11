package edu.unimagdalena.PulsePass.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.unimagdalena.PulsePass.domain.TicketStatus;
import edu.unimagdalena.PulsePass.domain.TicketType;
import edu.unimagdalena.PulsePass.dto.request.PurchaseTicketRequest;
import edu.unimagdalena.PulsePass.dto.response.TicketResponse;
import edu.unimagdalena.PulsePass.exception.BusinessRuleException;
import edu.unimagdalena.PulsePass.exception.GlobalExceptionHandler;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.service.TicketService;

@WebMvcTest(TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService service;

    // Escenario de referencia (sección 23): Andrea compra para CMF-2026
    private TicketResponse buildResponse(TicketStatus status) {
        return new TicketResponse(1L, "TKT-001", TicketType.VIP, new BigDecimal("150000.00"),
                status, LocalDateTime.of(2026, 10, 10, 12, 0),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    // TEST-CTRL-TKT-001 / AC-CTRL009
    @Test
    void shouldPurchaseTicket() throws Exception {
        when(service.purchase(any(PurchaseTicketRequest.class)))
                .thenReturn(buildResponse(TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userEmail": "andrea@email.com",
                                  "eventCode": "CMF-2026",
                                  "type": "VIP"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-001"))
                .andExpect(jsonPath("$.type").value("VIP"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.userEmail").value("andrea@email.com"))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(service).purchase(any(PurchaseTicketRequest.class));
    }

    // TEST-CTRL-TKT-002
    @Test
    void shouldReturn400WhenPurchaseRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userEmail": "",
                                  "eventCode": "",
                                  "type": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.userEmail").value("User email is required"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(service, never()).purchase(any());
    }

    // TEST-CTRL-TKT-003
    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        when(service.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User", "ghost@email.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userEmail": "ghost@email.com",
                                  "eventCode": "CMF-2026",
                                  "type": "GENERAL"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found: ghost@email.com"))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).purchase(any(PurchaseTicketRequest.class));
    }

    // TEST-CTRL-TKT-004 / AC-CTRL010
    @Test
    void shouldReturn409WhenPurchaseViolatesBusinessRule() throws Exception {
        when(service.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("User does not meet minimum age."));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userEmail": "laura@email.com",
                                  "eventCode": "CMF-2026",
                                  "type": "GENERAL"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User does not meet minimum age."))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).purchase(any(PurchaseTicketRequest.class));
    }

    // TEST-CTRL-TKT-005
    @Test
    void shouldReturnTicketByCode() throws Exception {
        when(service.findByCode("TKT-001")).thenReturn(buildResponse(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TKT-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.eventName").value("Caribbean Music Fest 2026"));

        verify(service).findByCode("TKT-001");
    }

    // TEST-CTRL-TKT-006
    @Test
    void shouldReturnTicketsByUserEmail() throws Exception {
        when(service.findByUserEmail("andrea@email.com"))
                .thenReturn(List.of(buildResponse(TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userEmail").value("andrea@email.com"));

        verify(service).findByUserEmail("andrea@email.com");
    }

    // TEST-CTRL-TKT-007
    @Test
    void shouldReturnPaidTicketsByEvent() throws Exception {
        when(service.findPaidTicketsByEvent("CMF-2026"))
                .thenReturn(List.of(buildResponse(TicketStatus.PAID)));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(service).findPaidTicketsByEvent("CMF-2026");
    }

    // TEST-CTRL-TKT-008 / AC-CTRL011
    @Test
    void shouldCancelTicket() throws Exception {
        when(service.cancel("TKT-001")).thenReturn(buildResponse(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TKT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TKT-001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(service).cancel("TKT-001");
    }

    // TEST-CTRL-TKT-009 / AC-CTRL012
    @Test
    void shouldReturn409WhenTicketCannotBeCancelled() throws Exception {
        when(service.cancel("TKT-001"))
                .thenThrow(new BusinessRuleException("Only PAID tickets can be cancelled"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TKT-001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only PAID tickets can be cancelled"))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).cancel("TKT-001");
    }

    // TEST-CTRL-TKT-010 / AC-CTRL013
    @Test
    void shouldMarkTicketAsUsed() throws Exception {
        when(service.markAsUsed("TKT-001")).thenReturn(buildResponse(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TKT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TKT-001"))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(service).markAsUsed("TKT-001");
    }

    // TEST-CTRL-TKT-011 / AC-CTRL014
    @Test
    void shouldReturn409WhenTicketCannotBeUsed() throws Exception {
        when(service.markAsUsed("TKT-001"))
                .thenThrow(new BusinessRuleException("Only PAID tickets can be used"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TKT-001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only PAID tickets can be used"))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).markAsUsed("TKT-001");
    }
}
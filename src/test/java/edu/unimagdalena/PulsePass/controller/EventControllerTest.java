package edu.unimagdalena.PulsePass.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.unimagdalena.PulsePass.domain.EventCategory;
import edu.unimagdalena.PulsePass.domain.EventStatus;
import edu.unimagdalena.PulsePass.dto.request.CreateEventRequest;
import edu.unimagdalena.PulsePass.dto.response.ArtistResponse;
import edu.unimagdalena.PulsePass.dto.response.EventResponse;
import edu.unimagdalena.PulsePass.dto.response.EventSummaryResponse;
import edu.unimagdalena.PulsePass.exception.BusinessRuleException;
import edu.unimagdalena.PulsePass.exception.GlobalExceptionHandler;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.service.AuthorizationService;
import edu.unimagdalena.PulsePass.service.EventService;

@WebMvcTest(EventController.class)
@Import({GlobalExceptionHandler.class, ControllerTestSecurityConfig.class})   
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService service;

    @MockitoBean
        private AuthorizationService authorizationService;


     private static final String VALID_EVENT_BODY = """
        {
          "eventCode": "CMF-2026",
          "name": "Caribbean Music Fest 2026",
          "description": "Festival de musica caribena",
          "category": "MUSIC",
          "eventDate": "2026-12-15T20:00:00",
          "minimumAge": 18,
          "venueCode": "VEN-SMR-01"
        }
        """;

    // Evento de referencia (sección 23.2): Caribbean Music Fest 2026
    private EventResponse buildResponse(EventStatus status, List<ArtistResponse> artists) {
        return new EventResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica caribena", EventCategory.MUSIC, status,
                LocalDateTime.of(2026, 12, 15, 20, 0), 18,
                "VEN-SMR-01", "Marina Convention Center", artists);
    }

    private EventSummaryResponse buildSummary() {
        return new EventSummaryResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 15, 20, 0), "Marina Convention Center");
    }

    // TEST-CTRL-EVT-001 / AC-CTRL003
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateEvent() throws Exception {
        when(service.create(any(CreateEventRequest.class)))
                .thenReturn(buildResponse(EventStatus.DRAFT, List.of()));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventCode": "CMF-2026",
                                  "name": "Caribbean Music Fest 2026",
                                  "description": "Festival de musica caribena",
                                  "category": "MUSIC",
                                  "eventDate": "2026-12-15T20:00:00",
                                  "minimumAge": 18,
                                  "venueCode": "VEN-SMR-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(service).create(any(CreateEventRequest.class));
    }

    // TEST-CTRL-EVT-002
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WhenCreateRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventCode": "",
                                  "name": "",
                                  "category": null,
                                  "eventDate": null,
                                  "minimumAge": null,
                                  "venueCode": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.name").value("Name is required"))
                .andExpect(jsonPath("$.details.category").value("Category is required"))
                .andExpect(jsonPath("$.details.eventDate").value("Event date is required"))
                .andExpect(jsonPath("$.details.minimumAge").value("Minimum age is required"))
                .andExpect(jsonPath("$.details.venueCode").value("Venue code is required"));

        verify(service, never()).create(any());
    }

    // TEST-CTRL-EVT-003
    @Test
    void shouldReturnEventByCode() throws Exception {
        when(service.findByCode("CMF-2026"))
                .thenReturn(buildResponse(EventStatus.PUBLISHED, List.of()));

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.minimumAge").value(18));

        verify(service).findByCode("CMF-2026");
    }

    // TEST-CTRL-EVT-004
    @Test
    void shouldReturn404WhenEventDoesNotExist() throws Exception {
        when(service.findByCode("CMF-999"))
                .thenThrow(new ResourceNotFoundException("Event", "CMF-999"));

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Event not found: CMF-999"))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).findByCode("CMF-999");
    }

    // TEST-CTRL-EVT-005
    @Test
    void shouldReturnPublishedEvents() throws Exception {
        when(service.findPublishedEvents()).thenReturn(List.of(buildSummary()));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(service).findPublishedEvents();
    }

    // TEST-CTRL-EVT-006 / AC-CTRL005
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldPublishEvent() throws Exception {
        when(service.publish("CMF-2026"))
                .thenReturn(buildResponse(EventStatus.PUBLISHED, List.of()));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(service).publish("CMF-2026");
    }

    // TEST-CTRL-EVT-007 
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn409WhenEventCannotBePublished() throws Exception {
        when(service.publish("CMF-2026"))
                .thenThrow(new BusinessRuleException("Only DRAFT events can be published"));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only DRAFT events can be published"))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).publish("CMF-2026");
    }

    // TEST-CTRL-EVT-008
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAddArtistToEvent() throws Exception {
        ArtistResponse solarBeat = new ArtistResponse(1L, "Solar Beat", "Colombia", "Pop", true);
        when(service.addArtist("CMF-2026", 1L))
                .thenReturn(buildResponse(EventStatus.DRAFT, List.of(solarBeat)));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.artists.length()").value(1))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        verify(service).addArtist("CMF-2026", 1L);
    }

    // TEST-CTRL-EVT-009
    @Test
    void shouldReturnEventsByArtist() throws Exception {
        when(service.findByArtist("Solar Beat")).thenReturn(List.of(buildSummary()));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(service).findByArtist("Solar Beat");
    }

    // T-SEC-002
        @Test
        void createEvent_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
        }

        // T-SEC-003
        @Test
        @WithMockUser(roles = "USER")
        void createEvent_asUser_returns403() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
        }
}

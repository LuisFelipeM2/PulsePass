package edu.unimagdalena.PulsePass.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.unimagdalena.PulsePass.dto.response.VenueResponse;
import edu.unimagdalena.PulsePass.exception.GlobalExceptionHandler;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.service.VenueService;

@WebMvcTest(VenueController.class)
@Import(GlobalExceptionHandler.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService service;

    private VenueResponse buildResponse() {
        return new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Calle 1 # 2-3", 3, true);
    }

    // TEST-CTRL-VEN-001
    @Test
    void shouldReturnVenueByCode() throws Exception {
        when(service.findByCode("VEN-SMR-01")).thenReturn(buildResponse());

        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.capacity").value(3));

        verify(service).findByCode("VEN-SMR-01");
    }

    // TEST-CTRL-VEN-002
    @Test
    void shouldReturn404WhenVenueDoesNotExist() throws Exception {
        when(service.findByCode("VEN-XXX-99"))
                .thenThrow(new ResourceNotFoundException("Venue", "VEN-XXX-99"));

        mockMvc.perform(get("/api/venues/{code}", "VEN-XXX-99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: VEN-XXX-99"))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).findByCode("VEN-XXX-99");
    }

    // TEST-CTRL-VEN-003
    @Test
    void shouldReturnActiveVenues() throws Exception {
        when(service.findActiveVenues()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(service).findActiveVenues();
    }
}
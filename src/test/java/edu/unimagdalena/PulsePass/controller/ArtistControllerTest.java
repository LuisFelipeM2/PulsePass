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

import edu.unimagdalena.PulsePass.dto.response.ArtistResponse;
import edu.unimagdalena.PulsePass.exception.GlobalExceptionHandler;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.service.ArtistService;

@WebMvcTest(ArtistController.class)
@Import(GlobalExceptionHandler.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService service;

    // Artistas de referencia (sección 23.3): Solar Beat, Neon Waves, Caribbean Sound
    private ArtistResponse buildResponse(Long id, String stageName) {
        return new ArtistResponse(id, stageName, "Colombia", "Pop", true);
    }

    // TEST-CTRL-ART-001
    @Test
    void shouldReturnArtistById() throws Exception {
        when(service.findById(1L)).thenReturn(buildResponse(1L, "Solar Beat"));

        mockMvc.perform(get("/api/artists/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.active").value(true));

        verify(service).findById(1L);
    }

    // TEST-CTRL-ART-002
    @Test
    void shouldReturn404WhenArtistDoesNotExist() throws Exception {
        when(service.findById(99L))
                .thenThrow(new ResourceNotFoundException("Artist", 99L));

        mockMvc.perform(get("/api/artists/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Artist not found: 99"))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).findById(99L);
    }

    // TEST-CTRL-ART-003
    @Test
    void shouldReturnArtistByStageName() throws Exception {
        when(service.findByStageName("Neon Waves"))
                .thenReturn(buildResponse(2L, "Neon Waves"));

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Neon Waves"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Neon Waves"));

        verify(service).findByStageName("Neon Waves");
    }

    // TEST-CTRL-ART-004
    @Test
    void shouldReturnActiveArtists() throws Exception {
        when(service.findActiveArtists()).thenReturn(List.of(
                buildResponse(3L, "Caribbean Sound"),
                buildResponse(2L, "Neon Waves"),
                buildResponse(1L, "Solar Beat")));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].stageName").value("Caribbean Sound"))
                .andExpect(jsonPath("$[2].stageName").value("Solar Beat"));

        verify(service).findActiveArtists();
    }
}
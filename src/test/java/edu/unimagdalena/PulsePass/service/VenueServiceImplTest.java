package edu.unimagdalena.PulsePass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.unimagdalena.PulsePass.domain.Venue;
import edu.unimagdalena.PulsePass.dto.response.VenueResponse;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.mapper.VenueMapper;
import edu.unimagdalena.PulsePass.repository.VenueRepository;
import edu.unimagdalena.PulsePass.service.impl.VenueServiceImpl;

@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenueMapper venueMapper;

    @InjectMocks
    private VenueServiceImpl venueService;

    private Venue buildVenue() {
        return Venue.builder()
                .id(1L)
                .code("VEN-SMR-01")
                .name("Marina Convention Center")
                .city("Santa Marta")
                .address("Calle 1 # 2-3")
                .capacity(3)
                .active(true)
                .build();
    }

    private VenueResponse buildResponse() {
        return new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Calle 1 # 2-3", 3, true);
    }

    // FR-SVC-001: consultar venue por código.
    @Test
    @DisplayName("findByCode: venue existente -> retorna el DTO")
    void findByCode_existingVenue_returnsResponse() {
        Venue venue = buildVenue();
        VenueResponse expected = buildResponse();
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(expected);

        VenueResponse result = venueService.findByCode("VEN-SMR-01");

        assertThat(result).isEqualTo(expected);
        verify(venueRepository).findByCode("VEN-SMR-01");
        verify(venueMapper).toResponse(venue);
    }

    // BR-VENUE-001: si el venue no existe, se lanza ResourceNotFoundException.
    @Test
    @DisplayName("findByCode: venue inexistente -> ResourceNotFoundException")
    void findByCode_missingVenue_throwsResourceNotFound() {
        when(venueRepository.findByCode("VEN-XXX-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueService.findByCode("VEN-XXX-99"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found: VEN-XXX-99");

    }

    // FR-SVC-002 / BR-VENUE-002: solo se devuelven los venues activos.
    @Test
    @DisplayName("findActiveVenues: retorna los venues activos como DTOs")
    void findActiveVenues_returnsMappedActiveVenues() {
        Venue venue = buildVenue();
        VenueResponse response = buildResponse();
        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(response);

        List<VenueResponse> result = venueService.findActiveVenues();

        assertThat(result).containsExactly(response);
        assertThat(result).allMatch(VenueResponse::active);
        verify(venueRepository).findByActiveTrueOrderByNameAsc();
        verify(venueRepository, never()).findAll(); // no debe traer venues inactivos
    }

    // BR-VENUE-002: si no hay venues activos, la lista sale vacía.
    @Test
    @DisplayName("findActiveVenues: sin venues activos -> lista vacía")
    void findActiveVenues_noActiveVenues_returnsEmptyList() {
        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());

        List<VenueResponse> result = venueService.findActiveVenues();

        assertThat(result).isEmpty();
        verifyNoInteractions(venueMapper);
    }
}

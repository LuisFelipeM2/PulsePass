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

import edu.unimagdalena.PulsePass.service.impl.ArtistServiceImpl;
import edu.unimagdalena.PulsePass.domain.Artist;
import edu.unimagdalena.PulsePass.dto.response.ArtistResponse;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.mapper.ArtistMapper;
import edu.unimagdalena.PulsePass.repository.ArtistRepository;

@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistMapper artistMapper;

    @InjectMocks
    private ArtistServiceImpl artistService;

    // Artistas de referencia del PRD (sección 47): Solar Beat, Neon Waves, Caribbean Sound
    private Artist buildArtist(Long id, String stageName) {
        return Artist.builder()
                .id(id)
                .stageName(stageName)
                .country("Colombia")
                .genre("Pop")
                .active(true)
                .build();
    }

    private ArtistResponse buildResponse(Long id, String stageName) {
        return new ArtistResponse(id, stageName, "Colombia", "Pop", true);
    }

    // FR-SVC-009: consultar artista por id.
    @Test
    @DisplayName("findById: artista existente -> retorna el DTO")
    void findById_existingArtist_returnsResponse() {
        // ARRANGE
        Artist artist = buildArtist(1L, "Solar Beat");
        ArtistResponse expected = buildResponse(1L, "Solar Beat");
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(expected);

        // ACT
        ArtistResponse result = artistService.findById(1L);

        // ASSERT
        assertThat(result).isEqualTo(expected);
        verify(artistRepository).findById(1L);
        verify(artistMapper).toResponse(artist);
    }

    // BR-ARTIST-001: si el artista no existe, se lanza ResourceNotFoundException.
    @Test
    @DisplayName("findById: artista inexistente -> ResourceNotFoundException")
    void findById_missingArtist_throwsResourceNotFound() {
        // ARRANGE
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> artistService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: 99");

        verifyNoInteractions(artistMapper);
    }

    // FR-SVC-009: consultar artista por nombre artístico.
    @Test
    @DisplayName("findByStageName: artista existente -> retorna el DTO")
    void findByStageName_existingArtist_returnsResponse() {
        // ARRANGE
        Artist artist = buildArtist(2L, "Neon Waves");
        ArtistResponse expected = buildResponse(2L, "Neon Waves");
        when(artistRepository.findByStageNameIgnoreCase("Neon Waves")).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(expected);

        // ACT
        ArtistResponse result = artistService.findByStageName("Neon Waves");

        // ASSERT
        assertThat(result).isEqualTo(expected);
        verify(artistRepository).findByStageNameIgnoreCase("Neon Waves");
        verify(artistMapper).toResponse(artist);
    }

    // BR-ARTIST-001: si el artista no existe, se lanza ResourceNotFoundException.
    @Test
    @DisplayName("findByStageName: artista inexistente -> ResourceNotFoundException")
    void findByStageName_missingArtist_throwsResourceNotFound() {
        // ARRANGE
        when(artistRepository.findByStageNameIgnoreCase("Ghost Band")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> artistService.findByStageName("Ghost Band"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: Ghost Band");

        verifyNoInteractions(artistMapper);
    }

    // FR-SVC-009 / BR-ARTIST-002: solo se devuelven los artistas activos.
    @Test
    @DisplayName("findActiveArtists: retorna los artistas activos como DTOs")
    void findActiveArtists_returnsMappedActiveArtists() {
        // ARRANGE
        Artist solar = buildArtist(1L, "Solar Beat");
        Artist neon = buildArtist(2L, "Neon Waves");
        Artist caribbean = buildArtist(3L, "Caribbean Sound");
        ArtistResponse solarDto = buildResponse(1L, "Solar Beat");
        ArtistResponse neonDto = buildResponse(2L, "Neon Waves");
        ArtistResponse caribbeanDto = buildResponse(3L, "Caribbean Sound");
        when(artistRepository.findByActiveTrueOrderByStageNameAsc())
                .thenReturn(List.of(caribbean, neon, solar));
        when(artistMapper.toResponse(caribbean)).thenReturn(caribbeanDto);
        when(artistMapper.toResponse(neon)).thenReturn(neonDto);
        when(artistMapper.toResponse(solar)).thenReturn(solarDto);

        // ACT
        List<ArtistResponse> result = artistService.findActiveArtists();

        // ASSERT
        assertThat(result).containsExactly(caribbeanDto, neonDto, solarDto);
        assertThat(result).allMatch(a -> Boolean.TRUE.equals(a.active()));
        verify(artistRepository).findByActiveTrueOrderByStageNameAsc();
        verify(artistRepository, never()).findAll(); // no debe traer artistas inactivos
    }

    // BR-ARTIST-002: si no hay artistas activos, la lista sale vacía.
    @Test
    @DisplayName("findActiveArtists: sin artistas activos -> lista vacía")
    void findActiveArtists_noActiveArtists_returnsEmptyList() {
        // ARRANGE
        when(artistRepository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of());

        // ACT
        List<ArtistResponse> result = artistService.findActiveArtists();

        // ASSERT
        assertThat(result).isEmpty();
        verifyNoInteractions(artistMapper);
    }
}
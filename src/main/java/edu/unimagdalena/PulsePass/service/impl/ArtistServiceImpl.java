package edu.unimagdalena.PulsePass.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.unimagdalena.PulsePass.domain.Artist;
import edu.unimagdalena.PulsePass.dto.response.ArtistResponse;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.mapper.ArtistMapper;
import edu.unimagdalena.PulsePass.repository.ArtistRepository;
import edu.unimagdalena.PulsePass.service.ArtistService;

// SRV-003: implementación de ArtistService.
// SRV-004: todas las operaciones son lecturas, por eso readOnly = true.
@Service 
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistMapper artistMapper;

    public ArtistServiceImpl(ArtistRepository artistRepository, ArtistMapper artistMapper) {
        this.artistRepository = artistRepository;
        this.artistMapper = artistMapper;
    }

    // FR-SVC-009: consultar artista por id.
    @Override
    @Transactional (readOnly = true)
    public ArtistResponse findById(Long id) {

        // BR-ARTIST-001: si el artista no existe, se lanza ResourceNotFoundException.
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artist", id));

        return artistMapper.toResponse(artist);
    }

    // FR-SVC-009: consultar artista por nombre artístico.
    @Override
    @Transactional(readOnly = true)
    public ArtistResponse findByStageName(String stageName) {

        // BR-ARTIST-001: si el artista no existe, se lanza ResourceNotFoundException.
        Artist artist = artistRepository.findByStageNameIgnoreCase(stageName)
                .orElseThrow(() -> new ResourceNotFoundException("Artist", stageName));

        return artistMapper.toResponse(artist);
    }

    // FR-SVC-009: consultar artistas activos.
    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponse> findActiveArtists() {

        // BR-ARTIST-002: solo retorna artistas con active = true.
        return artistRepository.findByActiveTrueOrderByStageNameAsc()
                .stream()
                .map(artistMapper::toResponse)
                .toList();
    }
}
package edu.unimagdalena.PulsePass.service;

import java.util.List;

import edu.unimagdalena.PulsePass.dto.response.ArtistResponse;

public interface ArtistService {
    ArtistResponse findById(Long id);
    
    ArtistResponse findByStageName(String stageName);

    List<ArtistResponse> findActiveArtists();
}
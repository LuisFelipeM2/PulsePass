package edu.unimagdalena.PulsePass.service;

import java.util.List;

import edu.unimagdalena.PulsePass.dto.response.VenueResponse;

public interface VenueService {
    VenueResponse findByCode(String code);
    List<VenueResponse> findActiveVenues();
}

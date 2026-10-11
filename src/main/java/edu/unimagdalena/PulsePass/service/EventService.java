package edu.unimagdalena.PulsePass.service;

import java.util.List;

import edu.unimagdalena.PulsePass.dto.request.CreateEventRequest;
import edu.unimagdalena.PulsePass.dto.response.EventResponse;
import edu.unimagdalena.PulsePass.dto.response.EventSummaryResponse;

public interface EventService {
    EventResponse create(CreateEventRequest request);

    EventResponse findByCode(String eventCode);

    List<EventSummaryResponse> findPublishedEvents();

    EventResponse publish(String eventCode);

    EventResponse addArtist(
        String eventCode,
        Long artistId
    );

    List<EventSummaryResponse> findByArtist(
        String stageName
    );
}
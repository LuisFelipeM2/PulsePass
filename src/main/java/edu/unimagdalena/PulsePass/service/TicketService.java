package edu.unimagdalena.PulsePass.service;

import java.util.List;

import edu.unimagdalena.PulsePass.dto.request.PurchaseTicketRequest;
import edu.unimagdalena.PulsePass.dto.response.TicketResponse;

public interface TicketService {
    
    TicketResponse purchase(
        PurchaseTicketRequest request
    );
    TicketResponse findByCode(
        String ticketCode
    );
    List<TicketResponse> findByUserEmail(
        String email
    );
    List<TicketResponse> findPaidTicketsByEvent(
        String eventCode
    );
    TicketResponse cancel(
        String ticketCode
    );
    TicketResponse markAsUsed(
        String ticketCode
    );
}
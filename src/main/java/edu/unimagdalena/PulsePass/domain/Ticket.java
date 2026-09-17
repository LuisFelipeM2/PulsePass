package edu.unimagdalena.PulsePass.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence. *;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "tickets")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Ticket {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "ticket_code", nullable = false, unique = true)
    private String ticketCode;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TicketType type;
 
    // BR-007/NFR-008: BigDecimal, nunca float/double
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;
 
    @Column(name = "purchase_date", nullable = false)
    private LocalDateTime purchaseDate;
 
    // BR-005: Ticket pertenece a EXACTAMENTE un User -> nullable = false
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    // BR-005: Ticket pertenece a EXACTAMENTE un Event -> nullable = false
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
}

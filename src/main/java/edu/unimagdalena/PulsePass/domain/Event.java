package edu.unimagdalena.PulsePass.domain;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

import jakarta.persistence. *;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "events")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Event {
 
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column (name = "event_code", nullable = false, unique = true)
    private String eventCode;
 
    @Column(name = "name", nullable = false)
    private String name;
 
    @Column(name = "description")
    private String description;
    
    // BR-008: enums por NOMBRE (STRING), nunca por ordinal
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private EventCategory category;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EventStatus status;
 
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
 
    @Column(name = "minimum_age", nullable = false)
    private Integer minimumAge;
 
    // FR-EVT-006: opcional, agregada en V3
    @Column(name = "streaming_url", length = 500)
    private String streamingUrl;
 
    // BR-001: todo Event pertenece a EXACTAMENTE un Venue -> nullable = false
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;
 
    
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_artists",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private Set<Artist> artists = new HashSet<>();
}
package edu.unimagdalena.PulsePass.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence. *;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "artists")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Artist {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "stage_name", nullable = false, unique = true)
    private String stageName;
 
    @Column(name = "country")
    private String country;
 
    @Column(name = "genre")
    private String genre;
 
    @Column(name = "active", nullable = false)
    private Boolean active;
 
    
    @Builder.Default
    @ManyToMany(mappedBy = "artists", fetch = FetchType.LAZY)
    private Set<Event> events = new HashSet<>();
}

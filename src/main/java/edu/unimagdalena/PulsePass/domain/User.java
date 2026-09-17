package edu.unimagdalena.PulsePass.domain;

import java.util.Set;
import java.util.HashSet;

import jakarta.persistence. *;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "users")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class User {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "username", nullable = false, unique = true)
    private String username;
 
    @Column(name = "email", nullable = false, unique = true)
    private String email;
 
    @Column(name = "active", nullable = false)
    private Boolean active;
 
    
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private UserProfile profile;
 
    
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Ticket> tickets = new HashSet<>();
}

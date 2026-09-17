package edu.unimagdalena.PulsePass.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.unimagdalena.PulsePass.domain.Artist;
import edu.unimagdalena.PulsePass.domain.Event;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
    // FR-ART-002: "stageName no puede repetirse." Criterio: "La BD rechaza duplicados."
    Optional<Artist> findByStageName(String stageName);

    // FR-ART-004: "Un artista puede participar en varios eventos." Criterio:
    // "Una consulta JPQL recupera los eventos del artista solicitado."
    // Mecanismo: @Query + JPQL con JOIN (seccion 14, fila "Eventos por artista"),
    @Query ("""
            SELECT DISTINCT e 
            FROM Event e JOIN e.artists a 
            WHERE a.stageName = :stageName
            """)
    List<Event> findEventsByArtistStageName(@Param ("stageName") String stageName);
 }

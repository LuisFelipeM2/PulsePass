package edu.unimagdalena.PulsePass.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.unimagdalena.PulsePass.domain.Event;
import edu.unimagdalena.PulsePass.domain.EventStatus;

public interface EventRepository extends JpaRepository<Event, Long> {
    // FR-EVT-002: "eventCode debe ser unico."
    // Mecanismo: Query Method (seccion 14, fila "Buscar evento por eventCode").
    Optional<Event> findByEventCode(String eventCode);

    // FR-EVT-005: "Consultar eventos PUBLISHED ordenados por fecha ascendente."
    // Mecanismo: Query Method (seccion 14, fila "Eventos publicados ordenados por fecha").
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    // FR-VEN-004: "Debe ser posible recuperar eventos asociados a un venue
    // usando su codigo de negocio."
    // Mecanismo: Query Method navegando relacion (seccion 14, fila "Eventos de un venue por venue.code").
    List<Event> findByVenue_Code(String venueCode);

    // FR-SRC-001: "Buscar eventos donde participe un artista por stageName."
    // Mecanismo: @Query + JPQL con JOIN (seccion 14, fila "Eventos por artista").
    @Query("""
            SELECT DISTINCT e 
            FROM Event e JOIN e.artists a 
            WHERE a.stageName = :stageName
            """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    // FR-SRC-002: "Buscar eventos de una ciudad en los que participe un
    // artista especifico." Criterio: "Filtra por venue.city y artist.stageName."
    // Mecanismo: @Query + JPQL con multiples asociaciones (seccion 14,
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE v.city = :city
            AND a.stageName = :stageName
            """)
    List<Event> findByCityAndArtistStageName(@Param("city") String city,
                                              @Param("stageName") String stageName);

    // FR-SRC-003: "Buscar eventos publicados posteriores a una fecha, en una
    // ciudad y cuyo artista contenga un texto." Criterio: "case-insensitive
    // para artista, usa DISTINCT y ordena por fecha."
    // Mecanismo: @Query + JPQL con filtros, DISTINCT y orden (seccion 14,
    // fila "Eventos recomendados").
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE e.status = 'PUBLISHED'
            AND e.eventDate > :afterDate
            AND v.city = :city
            AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistText, '%'))
            ORDER BY e.eventDate ASC
            """)
    List<Event> findRecommendedEvents(@Param("afterDate") LocalDateTime afterDate,
                                       @Param("city") String city,
                                       @Param("artistText") String artistText);
}
 

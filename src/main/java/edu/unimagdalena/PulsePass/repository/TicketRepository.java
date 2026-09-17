package edu.unimagdalena.PulsePass.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.unimagdalena.PulsePass.domain.Ticket;
import edu.unimagdalena.PulsePass.domain.TicketStatus;

public interface TicketRepository extends JpaRepository <Ticket, Long> {
     // FR-TKT-006: "Consultar tickets de un usuario por email y opcionalmente
    // por estado." Criterio: "Se navega Ticket -> User -> email."
    // Mecanismo: Query Method navegando relacion (seccion 14).
    // Version SIN filtro de estado (el "opcionalmente" de la descripcion):
    List<Ticket> findByUser_Email(String email);

    // Version CON filtro de estado, para cuando si se necesita filtrar:
    List<Ticket> findByUser_EmailAndStatus(String email, TicketStatus status);

    // FR-TKT-007: "Recuperar tickets PAID de un evento mediante eventCode."
    // Criterio: "Solo retorna tickets pagados del evento solicitado."
    // Mecanismo elegido: JPQL (la seccion 14 permite Query Method o JPQL,
    // "justificando eleccion"). 
    
    // Se eligio JPQL porque el estado PAID es
    // parte del REQUISITO DE NEGOCIO en si, no un filtro cualquiera que el
    // usuario decide, asi que fijarlo dentro de la consulta deja mas claro
    // el propósito del metodo que un Query Method generico con status como parametro.
    @Query("""
            SELECT t 
            FROM Ticket t 
            WHERE t.event.eventCode = :eventCode AND t.status = 'PAID'
            """)
    List<Ticket> findPaidTicketsByEventCode(@Param("eventCode") String eventCode);

     // FR-TKT-008: "Contar tickets PAID de un evento." Criterio: "La consulta
    // retorna un valor numerico correcto."
    // Mecanismo: @Query + JPQL con COUNT (seccion 14).
    @Query("""
            SELECT COUNT(t) 
            FROM Ticket t 
            WHERE t.event.eventCode = :eventCode AND t.status = 'PAID'
            """)
    long countPaidTicketsByEventCode(@Param("eventCode") String eventCode);

    // FR-SRC-004: "Consultar tickets cuyo evento sea posterior a una fecha."
    // Criterio: "Los resultados quedan ordenados cronologicamente."
    // Mecanismo: Query Method navegando relacion 
    List<Ticket> findByEvent_EventDateAfterOrderByEvent_EventDateAsc(LocalDateTime date);
    
} 
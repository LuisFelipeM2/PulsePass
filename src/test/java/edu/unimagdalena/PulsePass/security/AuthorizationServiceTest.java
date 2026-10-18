package edu.unimagdalena.PulsePass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.unimagdalena.PulsePass.domain.Role;
import edu.unimagdalena.PulsePass.domain.Ticket;
import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.repository.TicketRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;
import edu.unimagdalena.PulsePass.service.AuthorizationService;

@ExtendWith (MockitoExtension.class)
public class AuthorizationServiceTest {
    @Mock TicketRepository ticketRepository;
    @Mock UserRepository userRepository;
    @InjectMocks AuthorizationService service;

    private SecurityPrincipal principal(long id, Role role) {
        return new SecurityPrincipal(id, "u" + id, "h", true, Set.of(role));
    }

    private User user(long id, String email) {
        return User.builder().id(id).username("u" + id).email(email).active(true).build();
    }

    private Ticket ticketOf(long ownerId) {
        Ticket ticket = mock(Ticket.class);
        when(ticket.getUser()).thenReturn(user(ownerId, "dueño@email.com"));
        return ticket;
    }

    // ---- Tickets ----
    @Test
    void ownerShouldAccessOwnTicket() {
        Ticket ticket = ticketOf(1L);
        when(ticketRepository.findByTicketCode("T1")).thenReturn(Optional.of(ticket));
        assertThatCode(() -> service.requireTicketAccess(principal(1L, Role.USER), "T1"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldHideForeignTicketAsNotFound() {
        // UC-SEC-06 / T-SEC-009
        Ticket ticket = ticketOf(2L);
        when(ticketRepository.findByTicketCode("T1")).thenReturn(Optional.of(ticket));
        assertThatThrownBy(() -> service.requireTicketAccess(principal(1L, Role.USER), "T1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnNotFoundForMissingTicket() {
        when(ticketRepository.findByTicketCode("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireTicketAccess(principal(1L, Role.USER), "X"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void staffShouldAccessAnyTicketWithoutLookup() {
        assertThatCode(() -> service.requireTicketAccess(principal(9L, Role.STAFF), "T1"))
                .doesNotThrowAnyException();
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void shouldAllowCancelForOwnerAndAdminButNotForOthers() {
        Ticket ticket = ticketOf(1L);
        when(ticketRepository.findByTicketCode("T1")).thenReturn(Optional.of(ticket));

        assertThatCode(() -> service.requireTicketCancel(principal(1L, Role.USER), "T1"))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.requireTicketCancel(principal(9L, Role.ADMIN), "T1"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.requireTicketCancel(principal(2L, Role.USER), "T1"))
                .isInstanceOf(ResourceNotFoundException.class);
        // FR-SECTKT-05: STAFF no es dueño ni ADMIN
        assertThatThrownBy(() -> service.requireTicketCancel(principal(3L, Role.STAFF), "T1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- Compra ----
    @Test
    void shouldAllowPurchaseWithOwnEmailIgnoringCase() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "andrea@email.com")));
        assertThatCode(() -> service.requirePurchaseForSelf(principal(1L, Role.USER), "ANDREA@email.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectPurchaseWithForeignEmail() {
        // UC-SEC-05 / T-SEC-010
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "andrea@email.com")));
        assertThatThrownBy(() -> service.requirePurchaseForSelf(principal(1L, Role.USER), "carlos@email.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- Búsquedas de usuario ----
    @Test
    void shouldAllowSelfLookupAndHideOthers() {
        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(user(1L, "andrea@email.com")));

        assertThatCode(() -> service.requireSelfOrAdminByEmail(principal(1L, Role.USER), "andrea@email.com"))
                .doesNotThrowAnyException();
        // T-SEC-015
        assertThatThrownBy(() -> service.requireSelfOrAdminByEmail(principal(2L, Role.USER), "andrea@email.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminShouldLookUpAnyUserWithoutQuery() {
        assertThatCode(() -> service.requireSelfOrAdminByUsername(principal(9L, Role.ADMIN), "andrea"))
                .doesNotThrowAnyException();
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldHideUnknownUserAsNotFound() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireSelfOrAdminByUsername(principal(1L, Role.USER), "nadie"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- Borradores ----
    @Test
    void onlyStaffAndAdminShouldSeeUnpublishedEvents() {
        assertThat(service.canSeeUnpublishedEvents(null)).isFalse();
        assertThat(service.canSeeUnpublishedEvents(principal(1L, Role.USER))).isFalse();
        assertThat(service.canSeeUnpublishedEvents(principal(2L, Role.STAFF))).isTrue();
        assertThat(service.canSeeUnpublishedEvents(principal(3L, Role.ADMIN))).isTrue();
    }
}

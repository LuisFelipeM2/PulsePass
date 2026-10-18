package edu.unimagdalena.PulsePass.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.unimagdalena.PulsePass.domain.Ticket;
import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.repository.TicketRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;
import edu.unimagdalena.PulsePass.security.SecurityPrincipal;

@Service 
public class AuthorizationService {
     private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public AuthorizationService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    // ---------- Roles ----------
    public boolean isAdmin(SecurityPrincipal principal) {
        return hasAuthority(principal, "ROLE_ADMIN");
    }

    public boolean isStaffOrAdmin(SecurityPrincipal principal) {
        return isAdmin(principal) || hasAuthority(principal, "ROLE_STAFF");
    }

    // FR-SECEVT-02 / 06: solo STAFF y ADMIN ven eventos no publicados
    public boolean canSeeUnpublishedEvents(SecurityPrincipal principal) {
        return isStaffOrAdmin(principal);
    }

    // ---------- Tickets ----------

    // FR-SECTKT-01 / SEC006: el userEmail del request debe ser el del usuario autenticado
    @Transactional (readOnly = true)
    public void requirePurchaseForSelf(SecurityPrincipal principal, String userEmail) {
        if (principal == null || userEmail == null) {
            throw new AccessDeniedException("Operación no permitida");
        }
        User self = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AccessDeniedException("Operación no permitida"));
        if (!self.getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("Operación no permitida");
        }
    }

    // FR-SECTKT-02: dueño, o STAFF/ADMIN
    @Transactional(readOnly = true)
    public void requireTicketAccess(SecurityPrincipal principal, String ticketCode) {
        if (isStaffOrAdmin(principal)) return;
        requireTicketOwner(principal, ticketCode);
    }

    // FR-SECTKT-05: dueño, o ADMIN. Se llama ANTES de cambiar el estado (11.1)
    @Transactional(readOnly = true)
    public void requireTicketCancel(SecurityPrincipal principal, String ticketCode) {
        if (isAdmin(principal)) return;
        requireTicketOwner(principal, ticketCode);
    }

    // ---------- Usuarios (también sirve para tickets por correo, FR-SECTKT-03) ----------

    // FR-SECUSR-02 y FR-SECTKT-03: el propio usuario, o ADMIN
    @Transactional(readOnly = true)
    public void requireSelfOrAdminByEmail(SecurityPrincipal principal, String email) {
        if (isAdmin(principal)) return;
        User target = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        requireSameUser(principal, target);
    }

    // FR-SECUSR-03
    @Transactional(readOnly = true)
    public void requireSelfOrAdminByUsername(SecurityPrincipal principal, String username) {
        if (isAdmin(principal)) return;
        User target = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        requireSameUser(principal, target);
    }

    // ---------- Privados ----------

    private void requireTicketOwner(SecurityPrincipal principal, String ticketCode) {
        // 11.1: el dueño sale de la base de datos, el ticketCode solo no basta
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketCode));
        // Decisión del usuario: un ticket ajeno responde 404, igual que uno que no existe
        if (principal == null || !ticket.getUser().getId().equals(principal.getUserId())) {
            throw new ResourceNotFoundException("Ticket", ticketCode);
        }
    }

    private void requireSameUser(SecurityPrincipal principal, User target) {
        if (principal == null || !target.getId().equals(principal.getUserId())) {
            throw new ResourceNotFoundException("User", target.getId());
        }
    }

    private boolean hasAuthority(SecurityPrincipal principal, String authority) {
        return principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}

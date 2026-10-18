package edu.unimagdalena.PulsePass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import edu.unimagdalena.PulsePass.domain.Role;
import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.domain.UserCredential;
import edu.unimagdalena.PulsePass.domain.UserRole;
import edu.unimagdalena.PulsePass.domain.UserRoleId;
import edu.unimagdalena.PulsePass.repository.UserCredentialRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;
import edu.unimagdalena.PulsePass.repository.UserRoleRepository;


@ExtendWith (MockitoExtension.class)
public class CustomerDetailsServiceTest {
    @Mock UserRepository userRepository;
    @Mock UserCredentialRepository credentialRepository;
    @Mock UserRoleRepository roleRepository;

    @InjectMocks CustomUserDetailsService service;

    private User user(boolean active) {
        return User.builder()
                .id(1L)
                .username("andrea")
                .email("andrea@test.com")
                .active(active)
                .build();
    }

    private UserCredential credential() {
        UserCredential credential = new UserCredential();
        credential.setPasswordHash("$2a$10$hashDePrueba");
        return credential;
    }

    private UserRole userRole(Role role) {
        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(1L, role));
        return userRole;
    }

    @Test
    void shouldLoadExistingUserByUsername() {
        // 17.1: usuario existente
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user(true)));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(credential()));
        when(roleRepository.findByIdUserId(1L)).thenReturn(List.of(userRole(Role.USER)));

        SecurityPrincipal principal = (SecurityPrincipal) service.loadUserByUsername("andrea");

        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("andrea");
        assertThat(principal.getPassword()).isEqualTo("$2a$10$hashDePrueba");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        // 17.1: usuario inexistente
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void shouldReturnCurrentRolesFromDatabase() {
        // 17.1: roles actualizados. Los roles salen de la base, no de un token
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user(true)));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(credential()));
        when(roleRepository.findByIdUserId(1L))
                .thenReturn(List.of(userRole(Role.USER), userRole(Role.STAFF)));

        UserDetails principal = service.loadUserByUsername("andrea");

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_STAFF");
    }

    @Test
    void shouldMarkInactiveUserAsDisabled() {
        // 17.1: estado inactivo (FR-SECAUTH-004)
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user(false)));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(credential()));
        when(roleRepository.findByIdUserId(1L)).thenReturn(List.of(userRole(Role.USER)));

        UserDetails principal = service.loadUserByUsername("andrea");

        assertThat(principal.isEnabled()).isFalse();
    }

    @Test
    void shouldSearchByEmailWhenIdentifierContainsAt() {
        // FR-SECAUTH-002: si el identificador trae "@" se busca por correo
        when(userRepository.findByEmailIgnoreCase("andrea@test.com")).thenReturn(Optional.of(user(true)));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(credential()));
        when(roleRepository.findByIdUserId(1L)).thenReturn(List.of(userRole(Role.USER)));

        UserDetails principal = service.loadUserByUsername("andrea@test.com");

        assertThat(principal.getUsername()).isEqualTo("andrea");
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void shouldThrowWhenUserHasNoCredentials() {
        // 9.2: un usuario legado sin credencial no puede iniciar sesión
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user(true)));
        when(credentialRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("andrea"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void shouldLoadUserById() {
        // loadUserById: la carga que usará el filtro con el "sub" del token
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(true)));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(credential()));
        when(roleRepository.findByIdUserId(1L)).thenReturn(List.of(userRole(Role.ADMIN)));

        SecurityPrincipal principal = (SecurityPrincipal) service.loadUserById(1L);

        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }
}

package edu.unimagdalena.PulsePass.security;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import edu.unimagdalena.PulsePass.domain.Role;

public class SecurityPrincipal implements UserDetails {
      private final Long userId;
    private final String username;
    private final String passwordHash;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityPrincipal(Long userId, String username, String passwordHash,
                             boolean active, Set<Role> roles) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
        // FR-SECAUTH-009: los roles se convierten en authorities ROLE_USER, ROLE_STAFF, ROLE_ADMIN
        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    // 11.1: el control de propiedad compara contra este id, no contra un correo
    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // FR-SECAUTH-004: un usuario inactivo (active = false) queda deshabilitado
    @Override
    public boolean isEnabled() {
        return active;
    }
    
}

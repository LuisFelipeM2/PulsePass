package edu.unimagdalena.PulsePass.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.unimagdalena.PulsePass.domain.Role;
import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.domain.UserCredential;
import edu.unimagdalena.PulsePass.repository.UserCredentialRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;
import edu.unimagdalena.PulsePass.repository.UserRoleRepository;

@Service 
public class CustomUserDetailsService implements UserDetailsService {
      private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCredentialRepository credentialRepository,
                                    UserRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
    }

    // FR-SECAUTH-002: el login acepta usuario o correo.
    // Si el identificador trae "@" se busca por correo; si no, por username.
    @Override
    @Transactional (readOnly = true)
    public UserDetails loadUserByUsername(String identifier) {
        User user = (identifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(identifier)
                : userRepository.findByUsername(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return toPrincipal(user);
    }

    // FR-SECAUTH-009 / SEC011: con cada token se vuelve a cargar al usuario por su ID
    // (el "sub" del JWT), para tener su estado y sus roles actuales.
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return toPrincipal(user);
    }

    private SecurityPrincipal toPrincipal(User user) {
        // 9.2: un usuario legado no tiene credencial, así que no puede iniciar sesión
        UserCredential credential = credentialRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario sin credenciales"));

        // 12.1: los roles se leen siempre de la base de datos
        Set<Role> roles = roleRepository.findByIdUserId(user.getId()).stream()
                .map(userRole -> userRole.getId().getRole())
                .collect(Collectors.toSet());

        // FR-SECAUTH-004: el estado activo se toma del usuario guardado
        return new SecurityPrincipal(
                user.getId(),
                user.getUsername(),
                credential.getPasswordHash(),
                Boolean.TRUE.equals(user.getActive()),
                roles);
    }
    
}

package edu.unimagdalena.PulsePass.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.unimagdalena.PulsePass.domain.Role;
import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.domain.UserCredential;
import edu.unimagdalena.PulsePass.domain.UserRole;
import edu.unimagdalena.PulsePass.domain.UserRoleId;
import edu.unimagdalena.PulsePass.dto.request.LoginRequest;
import edu.unimagdalena.PulsePass.dto.request.RegisterUserRequest;
import edu.unimagdalena.PulsePass.dto.response.AuthResponse;
import edu.unimagdalena.PulsePass.dto.response.AuthenticatedUserResponse;
import edu.unimagdalena.PulsePass.dto.response.UserResponse;
import edu.unimagdalena.PulsePass.repository.UserCredentialRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;
import edu.unimagdalena.PulsePass.repository.UserRoleRepository;
import edu.unimagdalena.PulsePass.security.JwtProperties;
import edu.unimagdalena.PulsePass.security.JwtService;
import edu.unimagdalena.PulsePass.security.SecurityPrincipal;

@Service 
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       UserService userService,
                       UserRepository userRepository,
                       UserCredentialRepository credentialRepository,
                       UserRoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userService = userService;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // FR-SECAUTH-002: login con usuario o correo. El token solo se emite
    // después de que el AuthenticationManager acepta las credenciales.
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.identifier(), request.password()));

        SecurityPrincipal principal = (SecurityPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.getUserId());

        return new AuthResponse(token, "Bearer", jwtProperties.accessTokenTtl().toSeconds());
    }

    // FR-SECAUTH-010: datos mínimos del usuario autenticado, sin secretos
    public AuthenticatedUserResponse me(SecurityPrincipal principal) {
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return new AuthenticatedUserResponse(principal.getUserId(), principal.getUsername(), roles);
    }

    // FR-SECAUTH-001 / 9.3 / NFR-SEC004: usuario + hash + ROLE_USER en una sola
    // transacción. Si algo falla, no queda una cuenta sin credenciales.
    @Transactional 
    public UserResponse register(RegisterUserRequest request) {
        // UserService conserva sus reglas de negocio (unicidad, validaciones)
        UserResponse created = userService.register(request);

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("Usuario recién creado no encontrado"));

        // SEC003: solo se guarda el hash BCrypt
        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        credential.setPasswordChangedAt(LocalDateTime.now());
        credentialRepository.save(credential);

        // FR-SECUSR-01 / NFR-SEC009: el rol inicial es siempre USER, nunca viene del cliente
        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(user.getId(), Role.USER));
        userRole.setUser(user);
        roleRepository.save(userRole);

        return created;
    }
}

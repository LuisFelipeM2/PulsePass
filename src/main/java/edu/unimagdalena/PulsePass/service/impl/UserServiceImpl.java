package edu.unimagdalena.PulsePass.service.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.domain.UserProfile;
import edu.unimagdalena.PulsePass.dto.request.RegisterUserRequest;
import edu.unimagdalena.PulsePass.dto.response.UserResponse;
import edu.unimagdalena.PulsePass.exception.BusinessRuleException;
import edu.unimagdalena.PulsePass.exception.DuplicateResourceException;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.mapper.UserMapper;
import edu.unimagdalena.PulsePass.repository.UserProfileRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;
import edu.unimagdalena.PulsePass.service.UserService;

// SRV-003: implementación de UserService.
// SRV-004: escritura con @Transactional, lecturas con readOnly = true.
@Service 
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository,
                           UserProfileRepository userProfileRepository,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userMapper = userMapper;
    }

    // FR-SVC-010: registrar usuario con perfil (sección 20).
    // BR-USER-004: User y UserProfile se crean dentro de la misma transacción;
    // si algo falla, se revierte todo.
    @Override
    @Transactional 
    public UserResponse register(RegisterUserRequest request) {

        // BR-USER-001: el username debe ser único.
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists.");
        }

        // BR-USER-002: el email debe ser único ignorando mayúsculas y minúsculas.
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email already exists.");
        }
        // BR-USER-005: la fecha de nacimiento no puede ser futura.
        if (request.birthDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Birth date cannot be in the future.");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                // BR-USER-003: todo usuario nuevo inicia con active = true.
                .active(true)
                .build();
        User savedUser = userRepository.save(user);

        // BR-USER-004: el perfil se crea apuntando al usuario recién guardado.
        UserProfile profile = UserProfile.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .city(request.city())
                .birthDate(request.birthDate())
                .user(savedUser)
                .build();
        UserProfile savedProfile = userProfileRepository.save(profile);

        savedUser.setProfile(savedProfile);

        return userMapper.toResponse(savedUser);
    }

    // FR-SVC-011: consultar usuario por email (sin distinguir mayúsculas).
    @Override
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        return userMapper.toResponse(user);
    }

    // FR-SVC-012: consultar usuario por username.
    @Override
    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        return userMapper.toResponse(user);
    }
}
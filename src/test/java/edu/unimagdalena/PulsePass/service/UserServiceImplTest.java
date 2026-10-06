package edu.unimagdalena.PulsePass.service;

import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.domain.UserProfile;
import edu.unimagdalena.PulsePass.dto.request.RegisterUserRequest;
import edu.unimagdalena.PulsePass.dto.response.UserResponse;
import edu.unimagdalena.PulsePass.exception.BusinessRuleException;
import edu.unimagdalena.PulsePass.exception.DuplicateResourceException;
import edu.unimagdalena.PulsePass.mapper.UserMapper;
import edu.unimagdalena.PulsePass.repository.UserProfileRepository;
import edu.unimagdalena.PulsePass.repository.UserRepository;
import edu.unimagdalena.PulsePass.service.impl.UserServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Sección 42: unit test con el servicio real, Repository Mock y Mapper Mock.
// NFR-001: sin @SpringBootTest, sin PostgreSQL ni Testcontainers.
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String USERNAME = "andrea";
    private static final String EMAIL = "andrea@email.com";
    private static final LocalDate BIRTH_DATE = LocalDate.of(2000, 1, 1);

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private UserMapper userMapper;
    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<UserProfile> profileCaptor;

    @InjectMocks
    private UserServiceImpl userService;

    // TEST-USER-001: registrar usuario válido.
    @Test
    void register_validUser_savesUserAndProfile() {
        // ARRANGE
        UserResponse response = new UserResponse(1L, USERNAME, EMAIL, "Andrea", "Perez", true);
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(response);

        // ACT
        UserResponse result = userService.register(request(BIRTH_DATE));

        // ASSERT
        assertThat(result).isEqualTo(response);
        verify(userRepository).save(userCaptor.capture());
        verify(userProfileRepository).save(profileCaptor.capture());
        // BR-USER-003: todo usuario nuevo inicia con active = true.
        assertThat(userCaptor.getValue().getActive()).isTrue();
        // BR-USER-004: el perfil queda enlazado al usuario guardado.
        assertThat(profileCaptor.getValue().getUser()).isSameAs(userCaptor.getValue());
        assertThat(profileCaptor.getValue().getBirthDate()).isEqualTo(BIRTH_DATE);
    }

    // TEST-USER-002: username duplicado → DuplicateResourceException (BR-USER-001).
    @Test
    void register_duplicateUsername_throwsDuplicateAndNeverSaves() {
        // ARRANGE
        when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> userService.register(request(BIRTH_DATE)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists.");
        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    // TEST-USER-003: email duplicado → DuplicateResourceException (BR-USER-002).
    @Test
    void register_duplicateEmail_throwsDuplicateAndNeverSaves() {
        // ARRANGE
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> userService.register(request(BIRTH_DATE)))
                .isInstanceOf(DuplicateResourceException.class);
        // BR-USER-002: la comprobación del email ignora mayúsculas y minúsculas.
        verify(userRepository).existsByEmailIgnoreCase(eq(EMAIL));
        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    // TEST-USER-004: fecha de nacimiento futura → BusinessRuleException (BR-USER-005).
    @Test
    void register_futureBirthDate_throwsBusinessRuleAndNeverSaves() {
        // ARRANGE
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> userService.register(request(LocalDate.now().plusDays(1))))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    // --- Datos de prueba (Andrea, escenario de la sección 47) ---

    private RegisterUserRequest request(LocalDate birthDate) {
        return new RegisterUserRequest(USERNAME, EMAIL, "Andrea", "Perez",
                "3001234567", "Santa Marta", birthDate);
    }
}
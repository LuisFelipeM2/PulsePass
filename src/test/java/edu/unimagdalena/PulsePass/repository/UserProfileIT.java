package edu.unimagdalena.PulsePass.repository;

import edu.unimagdalena.PulsePass.TestcontainersConfiguration;
import edu.unimagdalena.PulsePass.domain.User;
import edu.unimagdalena.PulsePass.domain.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.LocalDate;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserProfileIT {
 
    @Autowired
    private UserRepository userRepository;
 
    @Autowired
    private UserProfileRepository userProfileRepository;
 
    @Test
    void deberiaGuardarYRecuperarUnPerfilAsociadoAUnUsuario() {
        User user = User.builder()
                .username("andrea.gomez")
                .email("andrea.gomez@example.com")
                .active(true)
                .build();
        userRepository.save(user);
 
        UserProfile perfil = UserProfile.builder()
                .firstName("Andrea")
                .lastName("Gomez")
                .phone("3001234567")
                .city("Santa Marta")
                .birthDate(LocalDate.of(1998, 5, 20))
                .user(user)
                .build();
 
        UserProfile guardado = userProfileRepository.save(perfil);
 
        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getUser().getUsername()).isEqualTo("andrea.gomez");
    }
 
    @Test
    void deberiaEncontrarUnPerfilPorElIdDelUsuario() {
        User user = User.builder()
                .username("carlos.perez")
                .email("carlos.perez@example.com")
                .active(true)
                .build();
        userRepository.save(user);
 
        UserProfile perfil = UserProfile.builder()
                .firstName("Carlos")
                .lastName("Perez")
                .phone("3007654321")
                .city("Barranquilla")
                .birthDate(LocalDate.of(1995, 11, 3))
                .user(user)
                .build();
        userProfileRepository.save(perfil);
 
        UserProfile encontrado = userProfileRepository.findByUser_Id(user.getId()).orElseThrow();
 
        assertThat(encontrado.getFirstName()).isEqualTo("Carlos");
        assertThat(encontrado.getCity()).isEqualTo("Barranquilla");
    }
 
    @Test
    void deberianRecuperarseTodosLosDatosDelPerfilPorElUsername() {
        User user = User.builder()
                .username("laura.diaz")
                .email("laura.diaz@example.com")
                .active(true)
                .build();
        userRepository.save(user);
 
        UserProfile perfil = UserProfile.builder()
                .firstName("Laura")
                .lastName("Diaz")
                .phone("3009876543")
                .city("Bogota")
                .birthDate(LocalDate.of(2000, 1, 15))
                .user(user)
                .build();
        userProfileRepository.save(perfil);
 
        UserProfile encontrado = userProfileRepository.findByUser_Username("laura.diaz").orElseThrow();
 
        assertThat(encontrado.getFirstName()).isEqualTo("Laura");
        assertThat(encontrado.getLastName()).isEqualTo("Diaz");
        assertThat(encontrado.getPhone()).isEqualTo("3009876543");
        assertThat(encontrado.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 15));
    }
 
    @Test
    void noDeberiaPermitirDosPerfilesParaElMismoUsuario() {
        User user = User.builder()
                .username("miguel.torres")
                .email("miguel.torres@example.com")
                .active(true)
                .build();
        userRepository.save(user);
 
        UserProfile primerPerfil = UserProfile.builder()
                .firstName("Miguel")
                .lastName("Torres")
                .city("Santa Marta")
                .birthDate(LocalDate.of(1990, 3, 10))
                .user(user)
                .build();
        userProfileRepository.saveAndFlush(primerPerfil);
 
        UserProfile segundoPerfil = UserProfile.builder()
                .firstName("Miguel")
                .lastName("Torres (duplicado)")
                .city("Santa Marta")
                .birthDate(LocalDate.of(1990, 3, 10))
                .user(user)
                .build();
 
        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(segundoPerfil))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
 
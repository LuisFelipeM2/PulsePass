package edu.unimagdalena.PulsePass.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.unimagdalena.PulsePass.dto.request.RegisterUserRequest;
import edu.unimagdalena.PulsePass.dto.response.UserResponse;
import edu.unimagdalena.PulsePass.exception.DuplicateResourceException;
import edu.unimagdalena.PulsePass.exception.GlobalExceptionHandler;
import edu.unimagdalena.PulsePass.service.UserService;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService service;

    // Usuario de referencia (sección 23.4): Andrea, adulta y activa
    private UserResponse buildResponse() {
        return new UserResponse(1L, "andrea", "andrea@email.com", "Andrea", "Perez", true);
    }

    // TEST-CTRL-USR-001 / AC-CTRL007
    @Test
    void shouldRegisterUser() throws Exception {
        when(service.register(any(RegisterUserRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "andrea",
                                  "email": "andrea@email.com",
                                  "firstName": "Andrea",
                                  "lastName": "Perez",
                                  "phone": "3001234567",
                                  "city": "Santa Marta",
                                  "birthDate": "2000-05-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(service).register(any(RegisterUserRequest.class));
    }

    // TEST-CTRL-USR-002
    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "andrea",
                                  "email": "not-an-email",
                                  "firstName": "Andrea",
                                  "lastName": "Perez",
                                  "birthDate": "2000-05-10"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.email").value("Email format is invalid"));

        verify(service, never()).register(any());
    }

    // TEST-CTRL-USR-003 / AC-CTRL008
    @Test
    void shouldReturn409WhenUsernameIsDuplicated() throws Exception {
        when(service.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists."));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "andrea",
                                  "email": "andrea@email.com",
                                  "firstName": "Andrea",
                                  "lastName": "Perez",
                                  "phone": "3001234567",
                                  "city": "Santa Marta",
                                  "birthDate": "2000-05-10"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already exists."))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).register(any(RegisterUserRequest.class));
    }

    // TEST-CTRL-USR-004
    @Test
    void shouldReturnUserByEmail() throws Exception {
        when(service.findByEmail("andrea@email.com")).thenReturn(buildResponse());

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.firstName").value("Andrea"));

        verify(service).findByEmail("andrea@email.com");
    }

    // TEST-CTRL-USR-005
    @Test
    void shouldReturnUserByUsername() throws Exception {
        when(service.findByUsername("andrea")).thenReturn(buildResponse());

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.active").value(true));

        verify(service).findByUsername("andrea");
    }
}
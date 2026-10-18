package edu.unimagdalena.PulsePass.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.springframework.security.access.AccessDeniedException;
import java.sql.Date;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

import edu.unimagdalena.PulsePass.domain.EventStatus;
import edu.unimagdalena.PulsePass.domain.Role;
import edu.unimagdalena.PulsePass.dto.request.LoginRequest;
import edu.unimagdalena.PulsePass.dto.response.AuthResponse;
import edu.unimagdalena.PulsePass.dto.response.EventResponse;
import edu.unimagdalena.PulsePass.exception.ResourceNotFoundException;
import edu.unimagdalena.PulsePass.service.ArtistService;
import edu.unimagdalena.PulsePass.service.AuthService;
import edu.unimagdalena.PulsePass.service.AuthorizationService;
import edu.unimagdalena.PulsePass.service.EventService;
import edu.unimagdalena.PulsePass.service.TicketService;
import edu.unimagdalena.PulsePass.service.UserService;
import edu.unimagdalena.PulsePass.service.VenueService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;


@WebMvcTest 
@Import ({SecurityConfig.class, JwtService.class,
         RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
public class SecurityRouteTest {
     // Clave exclusiva de pruebas (12.2 y 14.2)
    private static final SecretKey KEY = Jwts.SIG.HS256.key().build();
    private static final String SECRET = Encoders.BASE64.encode(KEY.getEncoded());
    private static final String ISSUER = "pulsepass-test";

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("pulsepass.security.jwt.issuer", () -> ISSUER);
        registry.add("pulsepass.security.jwt.secret-base64", () -> SECRET);
        registry.add("pulsepass.security.jwt.access-token-ttl", () -> "PT15M");
    }

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;

    @MockitoBean VenueService venueService;
    @MockitoBean EventService eventService;
    @MockitoBean ArtistService artistService;
    @MockitoBean UserService userService;
    @MockitoBean TicketService ticketService;
    @MockitoBean AuthService authService;
    @MockitoBean CustomUserDetailsService userDetailsService;
    @MockitoBean AuthorizationService authorizationService;

    private static final long USER_ID = 1L, STAFF_ID = 2L, ADMIN_ID = 3L, INACTIVE_ID = 4L;

    @BeforeEach
    void setUp() {
        when(userDetailsService.loadUserById(USER_ID))
                .thenReturn(new SecurityPrincipal(USER_ID, "andrea", "h", true, Set.of(Role.USER)));
        when(userDetailsService.loadUserById(STAFF_ID))
                .thenReturn(new SecurityPrincipal(STAFF_ID, "laura", "h", true, Set.of(Role.STAFF)));
        when(userDetailsService.loadUserById(ADMIN_ID))
                .thenReturn(new SecurityPrincipal(ADMIN_ID, "admin", "h", true, Set.of(Role.ADMIN)));
        when(userDetailsService.loadUserById(INACTIVE_ID))
                .thenReturn(new SecurityPrincipal(INACTIVE_ID, "inactivo", "h", false, Set.of(Role.USER)));
    }

    // ---------- Matriz de las 20 rutas (17.4) ----------

    enum Access { PUBLIC, AUTHENTICATED, ADMIN_ONLY, STAFF_OR_ADMIN, USER_ONLY }

    record Route(HttpMethod method, String path, Access access) {
        @Override public String toString() { return method + " " + path; }
    }

   static Stream<Route> routes() {
        return Stream.of(
                // Públicas
                new Route(HttpMethod.GET, "/api/venues/VEN-SMR-01", Access.PUBLIC),
                new Route(HttpMethod.GET, "/api/venues/active", Access.PUBLIC),
                new Route(HttpMethod.GET, "/api/events/published", Access.PUBLIC),
                new Route(HttpMethod.GET, "/api/artists/1", Access.PUBLIC),
                new Route(HttpMethod.GET, "/api/artists/by-stage-name?stageName=Solar%20Beat", Access.PUBLIC),
                new Route(HttpMethod.GET, "/api/artists/active", Access.PUBLIC),
                new Route(HttpMethod.POST, "/api/users", Access.PUBLIC),
                new Route(HttpMethod.GET, "/api/events/CMF-2026", Access.PUBLIC),
                new Route(HttpMethod.GET, "/api/events/by-artist?stageName=Solar%20Beat", Access.PUBLIC),
                new Route(HttpMethod.GET, "/api/users/by-email?email=andrea@email.com", Access.AUTHENTICATED),
                new Route(HttpMethod.GET, "/api/users/by-username?username=andrea", Access.AUTHENTICATED),
                 //USER_ONLY 
                new Route(HttpMethod.POST, "/api/tickets", Access.USER_ONLY),               
                new Route(HttpMethod.GET, "/api/tickets/TKT-1", Access.AUTHENTICATED),
                new Route(HttpMethod.GET, "/api/tickets/by-user?email=andrea@email.com", Access.AUTHENTICATED),
                new Route(HttpMethod.PATCH, "/api/tickets/TKT-1/cancel", Access.AUTHENTICATED),
                // Solo ADMIN
                new Route(HttpMethod.POST, "/api/events", Access.ADMIN_ONLY),
                new Route(HttpMethod.PATCH, "/api/events/CMF-2026/publish", Access.ADMIN_ONLY),
                new Route(HttpMethod.POST, "/api/events/CMF-2026/artists/1", Access.ADMIN_ONLY),
                // STAFF o ADMIN
                new Route(HttpMethod.GET, "/api/events/CMF-2026/tickets/paid", Access.STAFF_OR_ADMIN),
                new Route(HttpMethod.PATCH, "/api/tickets/TKT-1/use", Access.STAFF_OR_ADMIN));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("routes")
    void shouldApplyAccessMatrix(Route route) throws Exception {
        boolean isPublic = route.access() == Access.PUBLIC;

        // Anónimo
        int anonymous = call(route, null);
        if (isPublic) {
            assertReached(anonymous, route, "anónimo");
        } else {
            assertThat(anonymous).as("anónimo en %s", route).isEqualTo(401);
        }

        // USER, STAFF y ADMIN
        checkRole(route, USER_ID, "USER", Set.of("USER"));
        checkRole(route, STAFF_ID, "STAFF", Set.of("STAFF"));
        checkRole(route, ADMIN_ID, "ADMIN", Set.of("ADMIN"));
    }

    private void checkRole(Route route, long userId, String roleName, Set<String> own) throws Exception {
        boolean allowed = switch (route.access()) {
            case PUBLIC, AUTHENTICATED -> true;
            case ADMIN_ONLY -> own.contains("ADMIN");
            case STAFF_OR_ADMIN -> own.contains("STAFF") || own.contains("ADMIN");
            case USER_ONLY -> own.contains("USER"); // <-- Se agregó esta validación
        };
        int status = call(route, jwtService.generateToken(userId));
        if (allowed) {
            assertReached(status, route, roleName);
        } else {
            assertThat(status).as("%s en %s", roleName, route).isEqualTo(403);
        }
    }

    // Llegó al controlador: ni 401/403 (seguridad) ni 404/405 (ruta o verbo equivocado)
    private void assertReached(int status, Route route, String who) {
        assertThat(status).as("%s en %s", who, route).isNotIn(401, 403, 404, 405);
    }

    private int call(Route route, String token) throws Exception {
        MockHttpServletRequestBuilder builder = request(route.method(), route.path());
        if (route.method() != HttpMethod.GET) {
            builder.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(builder).andReturn().getResponse().getStatus();
    }

    // ---------- Formato de los errores y casos de token ----------

    @Test
    void shouldReturn401WithErrorResponseWhenNoToken() throws Exception {
        // T-SEC-002
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details").isMap());

        verifyNoInteractions(eventService);
    }

    @Test
    void shouldReturn403WithErrorResponseWhenUserCallsAdminRoute() throws Exception {
        // T-SEC-003
        mockMvc.perform(post("/api/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta operación"));

        verifyNoInteractions(eventService);
    }

    @Test
    void shouldReturn401WhenTokenIsExpired() throws Exception {
        // T-SEC-007
        Instant past = Instant.now().minusSeconds(3600);
        String expired = Jwts.builder().subject("1").issuer(ISSUER)
                .issuedAt(Date.from(past)).expiration(Date.from(past.plusSeconds(60)))
                .id(UUID.randomUUID().toString()).signWith(KEY, Jwts.SIG.HS256).compact();

        mockMvc.perform(get("/api/tickets/TKT-1").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(ticketService);
    }

    @Test
    void shouldReturn401WhenSignatureIsTampered() throws Exception {
        // T-SEC-008
        String[] parts = jwtService.generateToken(USER_ID).split("\\.");
        char[] signature = parts[2].toCharArray();
        signature[10] = signature[10] == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + new String(signature);

        mockMvc.perform(get("/api/tickets/TKT-1").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ticketService);
    }

    @Test
    void shouldReturn401WhenIssuerIsWrong() throws Exception {
        // UC-SEC-09
        String foreign = Jwts.builder().subject("1").issuer("otro-emisor")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(KEY, Jwts.SIG.HS256).compact();

        mockMvc.perform(get("/api/tickets/TKT-1").header(HttpHeaders.AUTHORIZATION, "Bearer " + foreign))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenUserIsInactive() throws Exception {
        // T-SEC-016 / UC-SEC-10
        mockMvc.perform(get("/api/tickets/TKT-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(INACTIVE_ID)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ticketService);
    }

    @Test
    void shouldReturn401NotServerErrorWhenBearerIsMalformed() throws Exception {
        // NFR-SEC002
        mockMvc.perform(get("/api/tickets/TKT-1").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotAcceptTokenInQueryString() throws Exception {
        // FR-SECAUTH-007
        mockMvc.perform(get("/api/tickets/TKT-1").param("access_token", jwtService.generateToken(USER_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnTokenOnLogin() throws Exception {
        // T-SEC-005
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse("tok", "Bearer", 900));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"andrea\",\"password\":\"UnaFraseSeguraLarga2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("tok"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void shouldReturnGeneric401WhenLoginFails() throws Exception {
        // T-SEC-006 / FR-SECAUTH-003
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("mala"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"andrea\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void shouldReturnAuthenticatedUserOnMe() throws Exception {
        // FR-SECAUTH-010: el principal llega al controller
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(USER_ID)))
                .andExpect(status().isOk());

        verify(authService).me(any(SecurityPrincipal.class));
    }
    private String userToken() {
    return "Bearer " + jwtService.generateToken(USER_ID);
}

@Test
void shouldReturn404WhenUserReadsForeignTicket() throws Exception {
    // T-SEC-009 / UC-SEC-06
    doThrow(new ResourceNotFoundException("Ticket", "TKT-9"))
            .when(authorizationService).requireTicketAccess(any(), eq("TKT-9"));

    mockMvc.perform(get("/api/tickets/TKT-9").header(HttpHeaders.AUTHORIZATION, userToken()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));

    verifyNoInteractions(ticketService);
}

@Test
void shouldReturn403AndCreateNothingWhenPurchasingForAnotherEmail() throws Exception {
    // T-SEC-010 / UC-SEC-05. Completa el JSON con los campos obligatorios de tu PurchaseTicketRequest
    doThrow(new AccessDeniedException("x"))
            .when(authorizationService).requirePurchaseForSelf(any(), eq("carlos@email.com"));

    mockMvc.perform(post("/api/tickets").header(HttpHeaders.AUTHORIZATION, userToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userEmail\":\"carlos@email.com\",\"eventCode\":\"CMF-2026\",\"type\":\"GENERAL\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));

    verifyNoInteractions(ticketService);
}

@Test
void shouldReturn404AndNotCancelForeignTicket() throws Exception {
    doThrow(new ResourceNotFoundException("Ticket", "TKT-9"))
            .when(authorizationService).requireTicketCancel(any(), eq("TKT-9"));

    mockMvc.perform(patch("/api/tickets/TKT-9/cancel").header(HttpHeaders.AUTHORIZATION, userToken()))
            .andExpect(status().isNotFound());

    verifyNoInteractions(ticketService);
}

@Test
void shouldReturn404WhenUserLooksUpAnotherUser() throws Exception {
    // T-SEC-015
    doThrow(new ResourceNotFoundException("User", "carlos@email.com"))
            .when(authorizationService).requireSelfOrAdminByEmail(any(), eq("carlos@email.com"));

    mockMvc.perform(get("/api/users/by-email").param("email", "carlos@email.com")
                    .header(HttpHeaders.AUTHORIZATION, userToken()))
            .andExpect(status().isNotFound());

    verifyNoInteractions(userService);
}

@Test
void shouldHideDraftEventFromAnonymous() throws Exception {
    // T-SEC-017 / UC-SEC-03. Ajusta DRAFT al nombre real de tu enum EventStatus
    EventResponse draft = mock(EventResponse.class);
    when(draft.status()).thenReturn(EventStatus.DRAFT);
    when(eventService.findByCode("CMF-2026")).thenReturn(draft);

    mockMvc.perform(get("/api/events/CMF-2026"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
}
}

package edu.unimagdalena.PulsePass.controller;

import java.time.LocalDateTime;
import java.util.Map;

import javax.naming.AuthenticationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.unimagdalena.PulsePass.dto.request.LoginRequest;
import edu.unimagdalena.PulsePass.dto.response.AuthResponse;
import edu.unimagdalena.PulsePass.dto.response.AuthenticatedUserResponse;
import edu.unimagdalena.PulsePass.dto.response.ErrorResponse;
import edu.unimagdalena.PulsePass.security.SecurityPrincipal;
import edu.unimagdalena.PulsePass.service.AuthService;

@RestController 
@RequestMapping ("/api/auth")
public class AuthController {
     private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 10.1: login público
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // 10.1: requiere Bearer. El principal lo deja listo el filtro JWT.
    @GetMapping("/me")
    public AuthenticatedUserResponse me(@AuthenticationPrincipal SecurityPrincipal principal) {
        return authService.me(principal);
    }

    // FR-SECAUTH-003: login incorrecto o usuario inactivo responde 401 genérico,
    // sin revelar si el usuario existe. Es local para que no caiga en el 500 del
    // handler general.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailure(AuthenticationException ex) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(), 401, "Unauthorized", "Credenciales inválidas", Map.of());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}

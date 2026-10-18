package edu.unimagdalena.PulsePass.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.unimagdalena.PulsePass.dto.request.RegisterUserRequest;
import edu.unimagdalena.PulsePass.dto.response.UserResponse;
import edu.unimagdalena.PulsePass.security.SecurityPrincipal;
import edu.unimagdalena.PulsePass.service.AuthService;
import edu.unimagdalena.PulsePass.service.AuthorizationService;
import edu.unimagdalena.PulsePass.service.UserService;
import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final AuthorizationService authorizationService;

    public UserController(UserService userService, AuthService authService, AuthorizationService authorizationService) {
        this.userService = userService;
        this.authService = authService;
        this.authorizationService = authorizationService; 
    }

    // FR-CTRL-USR-001: 201 Created; username o email duplicado -> 409 desde el handler.
    @PostMapping 
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    // FR-CTRL-USR-002: 200 o 404.
    @GetMapping ("/by-email")
    public ResponseEntity<UserResponse> findByEmail(@RequestParam String email, @AuthenticationPrincipal SecurityPrincipal principal) {
        authorizationService.requireSelfOrAdminByEmail(principal, email);
        return ResponseEntity.ok(userService.findByEmail(email));
    }

    // FR-CTRL-USR-003: 200 o 404.
    @GetMapping("/by-username")
    public ResponseEntity<UserResponse> findByUsername(@RequestParam String username,@AuthenticationPrincipal SecurityPrincipal principal) {
        authorizationService.requireSelfOrAdminByUsername(principal, username);
        return ResponseEntity.ok(userService.findByUsername(username));
    }
}
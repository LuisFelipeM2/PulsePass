package edu.unimagdalena.PulsePass.service;

import edu.unimagdalena.PulsePass.dto.request.RegisterUserRequest;
import edu.unimagdalena.PulsePass.dto.response.UserResponse;

public interface UserService {
    
    UserResponse register(RegisterUserRequest request);

    UserResponse findByEmail(String email);

    UserResponse findByUsername(String username);

}
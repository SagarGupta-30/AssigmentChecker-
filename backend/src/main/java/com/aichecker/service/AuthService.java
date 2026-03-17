package com.aichecker.service;

import com.aichecker.dto.AuthResponse;
import com.aichecker.dto.LoginRequest;
import com.aichecker.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}

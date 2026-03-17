package com.aichecker.service.impl;

import com.aichecker.dto.AuthResponse;
import com.aichecker.dto.LoginRequest;
import com.aichecker.dto.RegisterRequest;
import com.aichecker.dto.UserSummaryDto;
import com.aichecker.exception.ApiException;
import com.aichecker.model.User;
import com.aichecker.repository.UserRepository;
import com.aichecker.security.JwtService;
import com.aichecker.security.UserPrincipal;
import com.aichecker.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        User saved = userRepository.save(user);
        UserPrincipal principal = UserPrincipal.fromUser(saved);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, new UserSummaryDto(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getRole()
        ));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        UserPrincipal principal = UserPrincipal.fromUser(user);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, new UserSummaryDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        ));
    }
}

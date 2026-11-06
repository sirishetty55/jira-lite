package com.jiralite.service;

import com.jiralite.dto.request.LoginRequest;
import com.jiralite.dto.request.RegisterRequest;
import com.jiralite.dto.response.AuthResponse;
import com.jiralite.dto.response.UserResponse;
import com.jiralite.entity.User;
import com.jiralite.exception.DuplicateResourceException;
import com.jiralite.model.Role;
import com.jiralite.repository.UserRepository;
import com.jiralite.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration and authentication, issuing JWT tokens.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.DEVELOPER)   // default role
                .active(true)
                .build();

        user = userRepository.save(user);
        String token = tokenProvider.generateToken(user.getEmail());

        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        String token = tokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(request.email())
                .orElseThrow();

        return new AuthResponse(token, UserResponse.from(user));
    }
}


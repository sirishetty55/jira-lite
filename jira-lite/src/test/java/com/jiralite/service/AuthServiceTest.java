package com.jiralite.service;

import com.jiralite.dto.request.LoginRequest;
import com.jiralite.dto.request.RegisterRequest;
import com.jiralite.dto.response.AuthResponse;
import com.jiralite.entity.User;
import com.jiralite.exception.DuplicateResourceException;
import com.jiralite.model.Role;
import com.jiralite.repository.UserRepository;
import com.jiralite.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("encoded_pass")
                .role(Role.DEVELOPER)
                .active(true)
                .build();
        sampleUser.setId(1L);
    }

    @Test
    @DisplayName("register: succeeds when email is unique")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "password123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateToken("john@example.com")).thenReturn("jwt_token_xyz");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt_token_xyz");
        assertThat(response.user().email()).isEqualTo("john@example.com");
        assertThat(response.user().role()).isEqualTo(Role.DEVELOPER);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: throws DuplicateResourceException when email already exists")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "password123");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john@example.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login: succeeds with valid credentials")
    void login_Success() {
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("jwt_token_xyz");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt_token_xyz");
        assertThat(response.user().email()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("login: throws exception on invalid credentials")
    void login_BadCredentials_ThrowsException() {
        LoginRequest request = new LoginRequest("john@example.com", "wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}


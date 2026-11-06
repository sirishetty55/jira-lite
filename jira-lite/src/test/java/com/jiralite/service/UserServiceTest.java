package com.jiralite.service;

import com.jiralite.dto.response.UserResponse;
import com.jiralite.entity.User;
import com.jiralite.exception.BadRequestException;
import com.jiralite.exception.ResourceNotFoundException;
import com.jiralite.model.Role;
import com.jiralite.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@jiralite.com")
                .role(Role.DEVELOPER)
                .active(true)
                .build();
        activeUser.setId(10L);

        inactiveUser = User.builder()
                .firstName("Bob")
                .lastName("Jones")
                .email("bob@jiralite.com")
                .role(Role.DEVELOPER)
                .active(false)
                .build();
        inactiveUser.setId(11L);
    }

    @Test
    @DisplayName("getAllUsers: queries only active users, excluding deactivated accounts")
    void getAllUsers_QueriesOnlyActiveUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> activePage = new PageImpl<>(List.of(activeUser), pageable, 1);

        when(userRepository.findByActiveTrue(pageable)).thenReturn(activePage);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().email()).isEqualTo("alice@jiralite.com");
        assertThat(result.getContent().getFirst().active()).isTrue();

        verify(userRepository).findByActiveTrue(pageable);
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("getUserById: returns user response when user exists")
    void getUserById_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));

        UserResponse response = userService.getUserById(10L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.email()).isEqualTo("alice@jiralite.com");
    }

    @Test
    @DisplayName("getUserById: throws ResourceNotFoundException when user does not exist")
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("deleteUser: successfully deactivates user by setting active to false")
    void deleteUser_Success() {
        User adminUser = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@jiralite.com")
                .role(Role.ADMIN)
                .active(true)
                .build();
        adminUser.setId(1L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));

        userService.deleteUser(10L, adminUser);

        assertThat(activeUser.isActive()).isFalse();
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("deleteUser: throws BadRequestException when user attempts self-deactivation")
    void deleteUser_SelfDeactivation_ThrowsException() {
        assertThatThrownBy(() -> userService.deleteUser(10L, activeUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot deactivate your own account");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("getCurrentUser: resolves user from UserDetails email")
    void getCurrentUser_Success() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("alice@jiralite.com");
        when(userRepository.findByEmail("alice@jiralite.com")).thenReturn(Optional.of(activeUser));

        User user = userService.getCurrentUser(userDetails);

        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("alice@jiralite.com");
    }
}


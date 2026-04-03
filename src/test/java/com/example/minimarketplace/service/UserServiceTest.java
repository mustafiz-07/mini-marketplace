package com.example.minimarketplace.service;

import com.example.minimarketplace.dto.UserRequestDTO;
import com.example.minimarketplace.exception.ResourceNotFoundException;
import com.example.minimarketplace.model.Role;
import com.example.minimarketplace.model.User;
import com.example.minimarketplace.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterUser_whenEmailIsUnique() {
        // Arrange
        UserRequestDTO dto = new UserRequestDTO("Alice", "alice@example.com", "secret123", Role.BUYER);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(1L);
            return toSave;
        });

        // Act
        User saved = userService.register(dto);

        // Assert
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getPassword()).isEqualTo("encoded-secret");
        assertThat(saved.getRole()).isEqualTo(Role.BUYER);
        verify(userRepository).existsByEmail("alice@example.com");
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowException_whenRegisteringWithExistingEmail() {
        // Arrange
        UserRequestDTO dto = new UserRequestDTO("Alice", "alice@example.com", "secret123", Role.BUYER);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository).existsByEmail("alice@example.com");
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldReturnUser_whenFindByIdExists() {
        // Arrange
        User user = User.builder().id(10L).name("Bob").email("bob@example.com").password("x").role(Role.SELLER).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        // Act
        User found = userService.findById(10L);

        // Assert
        assertThat(found).isSameAs(user);
    }

    @Test
    void shouldThrowNotFound_whenFindByIdMissing() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
    }

    @Test
    void shouldReturnUser_whenFindByEmailExists() {
        // Arrange
        User user = User.builder().id(20L).name("Carol").email("carol@example.com").password("x").role(Role.BUYER).build();
        when(userRepository.findByEmail("carol@example.com")).thenReturn(Optional.of(user));

        // Act
        User found = userService.findByEmail("carol@example.com");

        // Assert
        assertThat(found).isSameAs(user);
    }

    @Test
    void shouldThrowNotFound_whenFindByEmailMissing() {
        // Arrange
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> userService.findByEmail("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with email: missing@example.com");
    }

    @Test
    void shouldReturnAllUsers_whenFindAllCalled() {
        // Arrange
        List<User> users = List.of(
                User.builder().id(1L).name("A").email("a@ex.com").password("x").role(Role.BUYER).build(),
                User.builder().id(2L).name("B").email("b@ex.com").password("x").role(Role.SELLER).build()
        );
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> found = userService.findAll();

        // Assert
        assertThat(found).hasSize(2).containsExactlyElementsOf(users);
    }

    @Test
    void shouldDeleteUser_whenDeleteByIdExists() {
        // Arrange
        when(userRepository.existsById(30L)).thenReturn(true);

        // Act
        userService.deleteById(30L);

        // Assert
        verify(userRepository).deleteById(30L);
    }

    @Test
    void shouldThrowNotFound_whenDeleteByIdMissing() {
        // Arrange
        when(userRepository.existsById(31L)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> userService.deleteById(31L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 31");

        verify(userRepository, never()).deleteById(any());
    }
}
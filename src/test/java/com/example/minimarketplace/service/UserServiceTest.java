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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
			User user = invocation.getArgument(0);
			user.setId(1L);
			return user;
		});

		// Act
		User result = userService.register(dto);

		// Assert
		assertEquals(1L, result.getId());
		assertEquals("encoded-secret", result.getPassword());
		verify(userRepository).save(any(User.class));
	}

	@Test
	void shouldThrowException_whenRegisterEmailAlreadyExists() {
		// Arrange
		UserRequestDTO dto = new UserRequestDTO("Alice", "alice@example.com", "secret123", Role.BUYER);
		when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

		// Act + Assert
		assertThrows(IllegalArgumentException.class, () -> userService.register(dto));
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void shouldReturnUser_whenFindByIdExists() {
		// Arrange
		User user = User.builder().id(10L).name("Bob").email("bob@example.com").password("x").role(Role.SELLER).build();
		when(userRepository.findById(10L)).thenReturn(Optional.of(user));

		// Act
		User result = userService.findById(10L);

		// Assert
		assertEquals(10L, result.getId());
	}

	@Test
	void shouldThrowException_whenFindByEmailMissing() {
		// Arrange
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> userService.findByEmail("missing@example.com"));
	}

	@Test
	void shouldReturnAllUsers_whenFindAllCalled() {
		// Arrange
		when(userRepository.findAll()).thenReturn(List.of(
				User.builder().id(1L).name("A").email("a@ex.com").password("x").role(Role.BUYER).build(),
				User.builder().id(2L).name("B").email("b@ex.com").password("x").role(Role.SELLER).build()
		));

		// Act
		List<User> users = userService.findAll();

		// Assert
		assertEquals(2, users.size());
	}

	@Test
	void shouldThrowException_whenDeleteByIdMissing() {
		// Arrange
		when(userRepository.existsById(99L)).thenReturn(false);

		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> userService.deleteById(99L));
		verify(userRepository, never()).deleteById(any());
	}
}

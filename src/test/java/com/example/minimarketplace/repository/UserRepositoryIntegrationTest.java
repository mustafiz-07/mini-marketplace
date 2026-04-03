package com.example.minimarketplace.repository;

import com.example.minimarketplace.model.Role;
import com.example.minimarketplace.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    @Test
    void shouldFindUserByEmail_whenUserExists() {
        // Arrange
        User user = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("encoded")
                .role(Role.BUYER)
                .build();
        userRepository.save(user);

        // Act
        var result = userRepository.findByEmail("alice@example.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice");
        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
    }
}
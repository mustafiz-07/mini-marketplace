package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.UserRequestDTO;
import com.example.minimarketplace.exception.GlobalExceptionHandler;
import com.example.minimarketplace.exception.ResourceNotFoundException;
import com.example.minimarketplace.model.Role;
import com.example.minimarketplace.model.User;
import com.example.minimarketplace.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private UserService userService;

    @Test
    void shouldRegisterUser_whenValidRequest() throws Exception {
        // Arrange
        UserRequestDTO request = new UserRequestDTO("Alice", "alice@example.com", "secret123", Role.BUYER);
        User saved = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .password("encoded")
                .role(Role.BUYER)
                .build();
        when(userService.register(request)).thenReturn(saved);

        // Act + Assert
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    void shouldReturnBadRequest_whenRegisterRequestInvalid() throws Exception {
        // Arrange
        UserRequestDTO invalid = new UserRequestDTO("", "not-email", "123", null);

        // Act + Assert
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists())
                .andExpect(jsonPath("$.role").exists());
    }

    @Test
    void shouldReturnUserById_whenUserExists() throws Exception {
        // Arrange
        User user = User.builder()
                .id(10L)
                .name("Bob")
                .email("bob@example.com")
                .password("encoded")
                .role(Role.SELLER)
                .build();
        when(userService.findById(10L)).thenReturn(user);

        // Act + Assert
        mockMvc.perform(get("/api/users/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Bob"))
                .andExpect(jsonPath("$.email").value("bob@example.com"))
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    void shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        // Arrange
        when(userService.findById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        // Act + Assert
        mockMvc.perform(get("/api/users/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("User not found with id: 99"));
    }

    @Test
    void shouldReturnAllUsers_whenRequestIsValid() throws Exception {
        // Arrange
        when(userService.findAll()).thenReturn(List.of(
                User.builder().id(1L).name("Alice").email("alice@example.com").password("x").role(Role.BUYER).build(),
                User.builder().id(2L).name("Bob").email("bob@example.com").password("x").role(Role.SELLER).build()
        ));

        // Act + Assert
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].role").value("SELLER"));
    }

    @Test
    void shouldDeleteUser_whenUserExists() throws Exception {
        // Arrange
        doNothing().when(userService).deleteById(3L);

        // Act + Assert
        mockMvc.perform(delete("/api/users/{id}", 3L))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFound_whenDeletingMissingUser() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User not found with id: 404"))
                .when(userService).deleteById(404L);

        // Act + Assert
        mockMvc.perform(delete("/api/users/{id}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("User not found with id: 404"));
    }
}
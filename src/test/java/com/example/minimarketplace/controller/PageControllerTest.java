package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.ProductResponseDTO;
import com.example.minimarketplace.dto.UserRequestDTO;
import com.example.minimarketplace.model.Role;
import com.example.minimarketplace.model.User;
import com.example.minimarketplace.repository.UserRepository;
import com.example.minimarketplace.service.OrderService;
import com.example.minimarketplace.service.ProductService;
import com.example.minimarketplace.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = PageController.class, properties = "spring.thymeleaf.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
class PageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldReturnHomeView_whenRequestIsValid() throws Exception {
        // Arrange
        when(productService.findAvailable()).thenReturn(List.of(productDto()));

        // Act + Assert
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("featuredProducts"))
                .andExpect(model().attribute("buyerId", 0L));
    }

    @Test
    void shouldReturnProductListView_whenSearchProvided() throws Exception {
        // Arrange
        when(productService.search("milk")).thenReturn(List.of(productDto()));

        // Act + Assert
        mockMvc.perform(get("/products").param("search", "milk"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-list"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attribute("search", "milk"));
    }

    @Test
    void shouldReturnProductDetailsView_whenProductExists() throws Exception {
        // Arrange
        when(productService.findById(5L)).thenReturn(productDto());

        // Act + Assert
        mockMvc.perform(get("/products/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(view().name("product-details"))
                .andExpect(model().attributeExists("product"));
    }

    @Test
    void shouldReturnAddProductView_whenRequestIsValid() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/add-product"))
                .andExpect(status().isOk())
                .andExpect(view().name("add-product"))
                .andExpect(model().attribute("buyerId", 0L));
    }

    @Test
    void shouldRedirectToLogin_whenRegistrationSucceeds() throws Exception {
        // Arrange
        when(userService.register(any(UserRequestDTO.class))).thenReturn(
                User.builder().id(1L).name("Alice").email("alice@example.com").password("encoded").role(Role.BUYER).build()
        );

        // Act + Assert
        mockMvc.perform(post("/register")
                        .param("name", "Alice")
                        .param("email", "alice@example.com")
                        .param("password", "secret123")
                        .param("role", "BUYER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void shouldReturnOrderHistoryView_whenRequestIsValid() throws Exception {
        // Arrange
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // Act + Assert
        mockMvc.perform(get("/order-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("order-history"))
                .andExpect(model().attribute("buyerId", 0L));
    }

    private ProductResponseDTO productDto() {
        return new ProductResponseDTO(
                1L,
                "Milk",
                "Fresh milk",
                new BigDecimal("2.50"),
                8,
                4L,
                "Seller"
        );
    }
}
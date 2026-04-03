package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.ProductRequestDTO;
import com.example.minimarketplace.dto.ProductResponseDTO;
import com.example.minimarketplace.exception.GlobalExceptionHandler;
import com.example.minimarketplace.exception.ResourceNotFoundException;
import com.example.minimarketplace.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private ProductService productService;

    @Test
    void shouldReturnSearchedProducts_whenSearchParamProvided() throws Exception {
        // Arrange
        when(productService.search("honey")).thenReturn(List.of(productDto(1L, "Organic Honey", 2L, "Seller A")));

        // Act + Assert
        mockMvc.perform(get("/api/products").param("search", "honey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Organic Honey"));
    }

    @Test
    void shouldReturnSellerProducts_whenSellerIdProvided() throws Exception {
        // Arrange
        when(productService.findBySeller(5L)).thenReturn(List.of(productDto(2L, "Milk", 5L, "Seller B")));

        // Act + Assert
        mockMvc.perform(get("/api/products").param("sellerId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sellerId").value(5L));
    }

    @Test
    void shouldReturnAvailableProducts_whenNoFiltersProvided() throws Exception {
        // Arrange
        when(productService.findAvailable()).thenReturn(List.of(productDto(3L, "Bread", 7L, "Seller C")));

        // Act + Assert
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bread"));
    }

    @Test
    void shouldReturnProductById_whenProductExists() throws Exception {
        // Arrange
        when(productService.findById(9L)).thenReturn(productDto(9L, "Tea", 2L, "Seller A"));

        // Act + Assert
        mockMvc.perform(get("/api/products/{id}", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9L))
                .andExpect(jsonPath("$.name").value("Tea"));
    }

    @Test
    void shouldReturnNotFound_whenProductMissing() throws Exception {
        // Arrange
        when(productService.findById(404L)).thenThrow(new ResourceNotFoundException("Product not found with id: 404"));

        // Act + Assert
        mockMvc.perform(get("/api/products/{id}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Product not found with id: 404"));
    }

    @Test
    void shouldCreateProduct_whenValidRequest() throws Exception {
        // Arrange
        ProductRequestDTO request = new ProductRequestDTO("Coffee", "Arabica", new BigDecimal("9.99"), 10);
        when(productService.create(request, 2L)).thenReturn(productDto(11L, "Coffee", 2L, "Seller A"));

        // Act + Assert
        mockMvc.perform(post("/api/products")
                        .param("sellerId", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11L))
                .andExpect(jsonPath("$.name").value("Coffee"));
    }

    @Test
    void shouldReturnBadRequest_whenCreateRequestInvalid() throws Exception {
        // Arrange
        ProductRequestDTO invalid = new ProductRequestDTO("", "Arabica", null, -1);

        // Act + Assert
        mockMvc.perform(post("/api/products")
                        .param("sellerId", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.quantity").exists());
    }

    @Test
    void shouldUpdateProduct_whenValidRequest() throws Exception {
        // Arrange
        ProductRequestDTO request = new ProductRequestDTO("Coffee Pro", "Arabica", new BigDecimal("10.99"), 15);
        when(productService.update(50L, request, 7L)).thenReturn(productDto(50L, "Coffee Pro", 7L, "Seller D"));

        // Act + Assert
        mockMvc.perform(put("/api/products/{id}", 50L)
                        .param("sellerId", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.name").value("Coffee Pro"));
    }

    @Test
    void shouldReturnBadRequest_whenUpdateUnauthorized() throws Exception {
        // Arrange
        ProductRequestDTO request = new ProductRequestDTO("Coffee Pro", "Arabica", new BigDecimal("10.99"), 15);
        when(productService.update(50L, request, 7L))
                .thenThrow(new IllegalArgumentException("You are not authorized to update this product."));

        // Act + Assert
        mockMvc.perform(put("/api/products/{id}", 50L)
                        .param("sellerId", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("You are not authorized to update this product."));
    }

    @Test
    void shouldDeleteProduct_whenRequestValid() throws Exception {
        // Arrange
        doNothing().when(productService).delete(70L, 3L);

        // Act + Assert
        mockMvc.perform(delete("/api/products/{id}", 70L).param("sellerId", "3"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnBadRequest_whenDeleteUnauthorized() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("You are not authorized to delete this product."))
                .when(productService).delete(70L, 3L);

        // Act + Assert
        mockMvc.perform(delete("/api/products/{id}", 70L).param("sellerId", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("You are not authorized to delete this product."));
    }

    private ProductResponseDTO productDto(Long id, String name, Long sellerId, String sellerName) {
        return new ProductResponseDTO(
                id,
                name,
                "desc",
                new BigDecimal("9.99"),
                10,
                sellerId,
                sellerName
        );
    }
}
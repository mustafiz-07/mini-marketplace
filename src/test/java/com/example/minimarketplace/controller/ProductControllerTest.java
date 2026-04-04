package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.ProductRequestDTO;
import com.example.minimarketplace.dto.ProductResponseDTO;
import com.example.minimarketplace.exception.GlobalExceptionHandler;
import com.example.minimarketplace.service.ProductService;
import com.example.minimarketplace.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockitoBean
    private UserService userService;

    @Test
    void shouldReturnSearchedProducts_whenSearchParamProvided() throws Exception {
        ProductResponseDTO dto = new ProductResponseDTO(1L, "Coffee", "Arabica", new BigDecimal("9.99"), 10, 2L, "Seller");
        when(productService.search("coffee")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/products").param("search", "coffee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Coffee"));
    }

    @Test
    void shouldReturnBadRequest_whenCreatePayloadInvalid() throws Exception {
        ProductRequestDTO invalid = new ProductRequestDTO("", "desc", null, -1);

        mockMvc.perform(post("/api/products")
                        .param("sellerId", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}

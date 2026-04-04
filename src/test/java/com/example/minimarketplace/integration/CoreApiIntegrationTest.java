package com.example.minimarketplace.integration;

import com.example.minimarketplace.dto.OrderRequestDTO;
import com.example.minimarketplace.dto.ProductRequestDTO;
import com.example.minimarketplace.dto.UserRequestDTO;
import com.example.minimarketplace.model.Product;
import com.example.minimarketplace.model.Role;
import com.example.minimarketplace.repository.OrderRepository;
import com.example.minimarketplace.repository.ProductRepository;
import com.example.minimarketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "DB_URL=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc(addFilters = false)
class CoreApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRetrieveUser_whenValidInput() throws Exception {
        UserRequestDTO request = new UserRequestDTO("Buyer One", "buyer.one@example.com", "secret123", Role.BUYER);

        MvcResult registerResult = mockMvc.perform(post("/api/users/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("buyer.one@example.com"))
                .andReturn();
        Long userId = readId(registerResult, "id");

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    void shouldCreateAndRetrieveProduct_whenSellerExists() throws Exception {
        Long sellerId = registerUser("Seller One", "seller.one@example.com", Role.SELLER);
        ProductRequestDTO request = new ProductRequestDTO("Organic Coffee", "500g beans", new BigDecimal("9.99"), 10);

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .param("sellerId", sellerId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Organic Coffee"))
                .andReturn();
        Long productId = readId(createResult, "id");

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId));
    }

    @Test
    void shouldUpdateProduct_whenOwnerMatchesSeller() throws Exception {
        Long sellerId = registerUser("Seller Two", "seller.two@example.com", Role.SELLER);
        Long productId = createProduct(sellerId, "Milk", new BigDecimal("2.10"), 5);
        ProductRequestDTO update = new ProductRequestDTO("Milk Premium", "2L", new BigDecimal("3.50"), 8);

        mockMvc.perform(put("/api/products/{id}", productId)
                        .param("sellerId", sellerId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Milk Premium"))
                .andExpect(jsonPath("$.quantity").value(8));
    }

    @Test
    void shouldDeleteProduct_whenOwnerMatchesSeller() throws Exception {
        Long sellerId = registerUser("Seller Three", "seller.three@example.com", Role.SELLER);
        Long productId = createProduct(sellerId, "Bread", new BigDecimal("1.50"), 4);

        mockMvc.perform(delete("/api/products/{id}", productId)
                        .param("sellerId", sellerId.toString()))
                .andExpect(status().isNoContent());

        assertFalse(productRepository.findById(productId).isPresent());
    }

    @Test
    void shouldPlaceOrderAndDeductStock_whenBuyerAndItemsValid() throws Exception {
        Long sellerId = registerUser("Seller Four", "seller.four@example.com", Role.SELLER);
        Long buyerId = registerUser("Buyer Four", "buyer.four@example.com", Role.BUYER);
        Long productId = createProduct(sellerId, "Tea", new BigDecimal("4.00"), 9);
        OrderRequestDTO request = new OrderRequestDTO(buyerId, List.of(new OrderRequestDTO.OrderItemDTO(productId, 3)));

        mockMvc.perform(post("/api/orders")
                        .with(user("buyer.four@example.com").roles("BUYER"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.buyerId").value(buyerId))
                .andExpect(jsonPath("$.itemCount").value(1));

        Product updated = productRepository.findById(productId).orElseThrow();
        assertEquals(6, updated.getQuantity());
    }

    private Long registerUser(String name, String email, Role role) throws Exception {
        UserRequestDTO request = new UserRequestDTO(name, email, "secret123", role);
        MvcResult result = mockMvc.perform(post("/api/users/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result, "id");
    }

    private Long createProduct(Long sellerId, String name, BigDecimal price, int quantity) throws Exception {
        ProductRequestDTO request = new ProductRequestDTO(name, "desc", price, quantity);
        MvcResult result = mockMvc.perform(post("/api/products")
                        .param("sellerId", sellerId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result, "id");
    }

    private Long readId(MvcResult result, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get(fieldName).asLong();
    }
}

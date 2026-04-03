package com.example.minimarketplace.integration;

import com.example.minimarketplace.dto.OrderRequestDTO;
import com.example.minimarketplace.dto.ProductRequestDTO;
import com.example.minimarketplace.dto.UserRequestDTO;
import com.example.minimarketplace.model.Order;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mini_marketplace")
            .withUsername("marketplace_user")
            .withPassword("marketplace_pass");

    @DynamicPropertySource
    static void configureDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
    }

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
    void shouldCompleteOrderFlowAcrossAllLayers_whenRequestsAreValid() throws Exception {
        // Arrange
        Long sellerId = registerUser("Seller One", "seller.one@example.com", Role.SELLER);
        Long buyerId = registerUser("Buyer One", "buyer.one@example.com", Role.BUYER);

        ProductRequestDTO productRequest = new ProductRequestDTO(
                "Organic Coffee",
                "500g beans",
                new BigDecimal("9.99"),
                10
        );

        // Act: create product through HTTP -> controller -> service -> repository -> DB
        MvcResult createProductResult = mockMvc.perform(post("/api/products")
                        .param("sellerId", sellerId.toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Organic Coffee"))
                .andExpect(jsonPath("$.sellerId").value(sellerId))
                .andReturn();

        Long productId = readId(createProductResult, "id");

        OrderRequestDTO orderRequest = new OrderRequestDTO(
                buyerId,
                List.of(new OrderRequestDTO.OrderItemDTO(productId, 2))
        );

        MvcResult placeOrderResult = mockMvc.perform(post("/api/orders")
                        .with(user("buyer.one@example.com").roles("BUYER"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.buyerId").value(buyerId))
                .andExpect(jsonPath("$.itemCount").value(1))
                .andReturn();

        Long orderId = readId(placeOrderResult, "orderId");

        // Assert: verify persisted state in DB
        Product persistedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(persistedProduct.getQuantity()).isEqualTo(8);

        Order persistedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(persistedOrder.getBuyerId()).isEqualTo(buyerId);
        assertThat(persistedOrder.getTotalPrice()).isEqualByComparingTo("19.98");

        // Assert: verify fetch endpoint works with security role
        mockMvc.perform(get("/api/orders/buyer/{buyerId}", buyerId)
                        .with(user("buyer.one@example.com").roles("BUYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId));
    }

    private Long registerUser(String name, String email, Role role) throws Exception {
        UserRequestDTO request = new UserRequestDTO(name, email, "secret123", role);
        MvcResult result = mockMvc.perform(post("/api/users/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value(role.name()))
                .andReturn();

        return readId(result, "id");
    }

    private Long readId(MvcResult result, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get(fieldName).asLong();
    }
}
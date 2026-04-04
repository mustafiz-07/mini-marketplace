package com.example.minimarketplace.integration;

import com.example.minimarketplace.model.Product;
import com.example.minimarketplace.model.Role;
import com.example.minimarketplace.model.User;
import com.example.minimarketplace.repository.OrderRepository;
import com.example.minimarketplace.repository.ProductRepository;
import com.example.minimarketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "DB_URL=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc(addFilters = false)
class PageTemplateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void shouldRenderHomePageWithOnlyAvailableFeaturedProducts() throws Exception {
        Long sellerId = createSeller("Home Seller", "home.seller@example.com");
        createProduct("Organic Honey", "Raw local honey", new BigDecimal("7.50"), 5, sellerId);
        createProduct("Out Of Stock Item", "Not available", new BigDecimal("3.00"), 0, sellerId);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("Featured Products")))
                .andExpect(content().string(containsString("Organic Honey")))
                .andExpect(content().string(not(containsString("Out Of Stock Item"))));
    }

    @Test
    void shouldRenderProductDetailsPageWithProductData() throws Exception {
        Long sellerId = createSeller("Seller One", "seller.one@example.com");
        Long productId = createProduct("Premium Tea", "Single origin tea", new BigDecimal("12.50"), 3, sellerId);

        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(view().name("product-details"))
                .andExpect(content().string(containsString("Premium Tea")))
                .andExpect(content().string(containsString("Sold by Seller One")))
                .andExpect(content().string(containsString("3 in stock")))
                .andExpect(content().string(containsString("$12.50")));
    }

    @Test
    void shouldRenderProductListPageWithSearchResults() throws Exception {
        Long sellerId = createSeller("Search Seller", "search.seller@example.com");
        createProduct("Colombian Coffee", "Dark roast", new BigDecimal("8.90"), 4, sellerId);

        mockMvc.perform(get("/products").param("search", "coffee"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-list"))
                .andExpect(content().string(containsString("All Products")))
                .andExpect(content().string(containsString("Colombian Coffee")))
                .andExpect(content().string(containsString("Showing results for")));
    }

    private Long createSeller(String name, String email) {
        User seller = userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password("encoded-password")
                .role(Role.SELLER)
                .build());
        return seller.getId();
    }

    private Long createProduct(String name, String description, BigDecimal price, int quantity, Long sellerId) {
        Product product = productRepository.save(Product.builder()
                .name(name)
                .description(description)
                .price(price)
                .quantity(quantity)
                .sellerId(sellerId)
                .build());
        return product.getId();
    }
}

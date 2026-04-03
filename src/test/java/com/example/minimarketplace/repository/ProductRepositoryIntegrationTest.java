package com.example.minimarketplace.repository;

import com.example.minimarketplace.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void clean() {
        productRepository.deleteAll();
    }

    @Test
    void shouldReturnProductsBySellerAndAvailabilityAndKeyword_whenDataExists() {
        // Arrange
        Product available = Product.builder()
                .name("Organic Honey")
                .description("Forest honey")
                .price(new BigDecimal("12.00"))
                .quantity(5)
                .sellerId(20L)
                .build();
        Product unavailable = Product.builder()
                .name("Milk")
                .description("Out of stock")
                .price(new BigDecimal("2.50"))
                .quantity(0)
                .sellerId(20L)
                .build();
        productRepository.save(available);
        productRepository.save(unavailable);

        // Act
        var bySeller = productRepository.findBySellerId(20L);
        var availableOnly = productRepository.findByQuantityGreaterThan(0);
        var keyword = productRepository.searchByKeyword("honey");

        // Assert
        assertThat(bySeller).hasSize(2);
        assertThat(availableOnly).hasSize(1);
        assertThat(availableOnly.getFirst().getName()).isEqualTo("Organic Honey");
        assertThat(keyword).hasSize(1);
        assertThat(keyword.getFirst().getDescription()).contains("Forest");
    }
}
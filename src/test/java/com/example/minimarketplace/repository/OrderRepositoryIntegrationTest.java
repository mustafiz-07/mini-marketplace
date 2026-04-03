package com.example.minimarketplace.repository;

import com.example.minimarketplace.model.Order;
import com.example.minimarketplace.model.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void clean() {
        orderRepository.deleteAll();
    }

    @Test
    void shouldReturnOrdersByBuyerWithItems_whenOrdersExist() {
        // Arrange
        Order first = Order.builder()
                .buyerId(30L)
                .orderDate(LocalDateTime.now().minusDays(1))
                .totalPrice(new BigDecimal("9.99"))
                .items(new ArrayList<>())
                .build();
        OrderItem firstItem = OrderItem.builder().productId(100L).quantity(1).order(first).build();
        first.getItems().add(firstItem);

        Order second = Order.builder()
                .buyerId(30L)
                .orderDate(LocalDateTime.now())
                .totalPrice(new BigDecimal("19.98"))
                .items(new ArrayList<>())
                .build();
        OrderItem secondItem = OrderItem.builder().productId(101L).quantity(2).order(second).build();
        second.getItems().add(secondItem);

        orderRepository.save(first);
        orderRepository.save(second);

        // Act
        var ordered = orderRepository.findByBuyerIdOrderByOrderDateDesc(30L);
        var withItems = orderRepository.findByBuyerIdWithItems(30L);

        // Assert
        assertThat(ordered).hasSize(2);
        assertThat(ordered.getFirst().getOrderDate()).isAfter(ordered.get(1).getOrderDate());
        assertThat(withItems).isNotEmpty();
        assertThat(withItems.getFirst().getItems()).isNotEmpty();
    }
}
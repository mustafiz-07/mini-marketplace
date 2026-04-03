package com.example.minimarketplace.service;

import com.example.minimarketplace.dto.OrderRequestDTO;
import com.example.minimarketplace.exception.ResourceNotFoundException;
import com.example.minimarketplace.model.Order;
import com.example.minimarketplace.model.Product;
import com.example.minimarketplace.repository.OrderRepository;
import com.example.minimarketplace.repository.ProductRepository;
import com.example.minimarketplace.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldPlaceOrder_whenBuyerAndProductsAreValid() {
        // Arrange
        Long buyerId = 1L;
        Long productId = 10L;
        Product product = Product.builder()
                .id(productId)
                .name("Coffee Beans")
                .price(new BigDecimal("5.50"))
                .quantity(10)
                .sellerId(2L)
                .build();

        OrderRequestDTO dto = new OrderRequestDTO(
                buyerId,
                List.of(new OrderRequestDTO.OrderItemDTO(productId, 2))
        );

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(new com.example.minimarketplace.model.User()));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(900L);
            if (order.getOrderDate() == null) {
                order.setOrderDate(LocalDateTime.now());
            }
            return order;
        });

        // Act
        Order placed = orderService.placeOrder(dto);

        // Assert
        assertThat(placed.getId()).isEqualTo(900L);
        assertThat(placed.getBuyerId()).isEqualTo(buyerId);
        assertThat(placed.getItems()).hasSize(1);
        assertThat(placed.getItems().getFirst().getProductId()).isEqualTo(productId);
        assertThat(placed.getItems().getFirst().getQuantity()).isEqualTo(2);
        assertThat(placed.getTotalPrice()).isEqualByComparingTo("11.00");
        assertThat(product.getQuantity()).isEqualTo(8);
        verify(productRepository).save(product);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldThrowNotFound_whenBuyerDoesNotExist() {
        // Arrange
        OrderRequestDTO dto = new OrderRequestDTO(
                999L,
                List.of(new OrderRequestDTO.OrderItemDTO(10L, 1))
        );
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> orderService.placeOrder(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Buyer not found with id: 999");

        verify(productRepository, never()).findById(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldThrowNotFound_whenProductDoesNotExist() {
        // Arrange
        Long buyerId = 1L;
        OrderRequestDTO dto = new OrderRequestDTO(
                buyerId,
                List.of(new OrderRequestDTO.OrderItemDTO(404L, 1))
        );

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(new com.example.minimarketplace.model.User()));
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> orderService.placeOrder(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 404");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldThrowIllegalState_whenStockIsInsufficient() {
        // Arrange
        Long buyerId = 1L;
        Long productId = 10L;
        Product product = Product.builder()
                .id(productId)
                .name("Coffee Beans")
                .price(new BigDecimal("5.50"))
                .quantity(1)
                .sellerId(2L)
                .build();
        OrderRequestDTO dto = new OrderRequestDTO(
                buyerId,
                List.of(new OrderRequestDTO.OrderItemDTO(productId, 2))
        );

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(new com.example.minimarketplace.model.User()));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act + Assert
        assertThatThrownBy(() -> orderService.placeOrder(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock for product");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldReturnOrder_whenFindByIdExists() {
        // Arrange
        Order order = Order.builder().id(100L).buyerId(1L).totalPrice(BigDecimal.ONE).build();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        // Act
        Order found = orderService.findById(100L);

        // Assert
        assertThat(found).isSameAs(order);
    }

    @Test
    void shouldThrowNotFound_whenFindByIdMissing() {
        // Arrange
        when(orderRepository.findById(123L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> orderService.findById(123L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 123");
    }

    @Test
    void shouldReturnOrdersByBuyer_whenBuyerHasOrders() {
        // Arrange
        List<Order> orders = List.of(
                Order.builder().id(1L).buyerId(7L).totalPrice(new BigDecimal("4.00")).build(),
                Order.builder().id(2L).buyerId(7L).totalPrice(new BigDecimal("8.50")).build()
        );
        when(orderRepository.findByBuyerIdWithItems(7L)).thenReturn(orders);

        // Act
        List<Order> result = orderService.findByBuyer(7L);

        // Assert
        assertThat(result).hasSize(2).containsExactlyElementsOf(orders);
    }

    @Test
    void shouldReturnAllOrders_whenFindAllCalled() {
        // Arrange
        List<Order> orders = List.of(
                Order.builder().id(1L).buyerId(7L).totalPrice(new BigDecimal("4.00")).build()
        );
        when(orderRepository.findAll()).thenReturn(orders);

        // Act
        List<Order> result = orderService.findAll();

        // Assert
        assertThat(result).hasSize(1).containsExactlyElementsOf(orders);
    }
}
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
	void shouldPlaceOrder_whenBuyerAndStockAreValid() {
		// Arrange
		Long buyerId = 1L;
		Product product = Product.builder().id(10L).name("Coffee").price(new BigDecimal("5.50")).quantity(10).sellerId(2L).build();
		OrderRequestDTO dto = new OrderRequestDTO(buyerId, List.of(new OrderRequestDTO.OrderItemDTO(10L, 2)));

		when(userRepository.findById(buyerId)).thenReturn(Optional.of(com.example.minimarketplace.model.User.builder().id(buyerId).build()));
		when(productRepository.findById(10L)).thenReturn(Optional.of(product));
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
			Order order = invocation.getArgument(0);
			order.setId(700L);
			return order;
		});

		// Act
		Order result = orderService.placeOrder(dto);

		// Assert
		assertEquals(700L, result.getId());
		assertEquals(new BigDecimal("11.00"), result.getTotalPrice());
		assertEquals(8, product.getQuantity());
	}

	@Test
	void shouldThrowException_whenBuyerMissing() {
		// Arrange
		OrderRequestDTO dto = new OrderRequestDTO(99L, List.of(new OrderRequestDTO.OrderItemDTO(10L, 1)));
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(dto));
	}

	@Test
	void shouldThrowException_whenProductMissing() {
		// Arrange
		OrderRequestDTO dto = new OrderRequestDTO(1L, List.of(new OrderRequestDTO.OrderItemDTO(10L, 1)));
		when(userRepository.findById(1L)).thenReturn(Optional.of(com.example.minimarketplace.model.User.builder().id(1L).build()));
		when(productRepository.findById(10L)).thenReturn(Optional.empty());

		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(dto));
	}

	@Test
	void shouldThrowException_whenStockInsufficient() {
		// Arrange
		Product product = Product.builder().id(10L).name("Coffee").price(new BigDecimal("5.50")).quantity(1).sellerId(2L).build();
		OrderRequestDTO dto = new OrderRequestDTO(1L, List.of(new OrderRequestDTO.OrderItemDTO(10L, 2)));
		when(userRepository.findById(1L)).thenReturn(Optional.of(com.example.minimarketplace.model.User.builder().id(1L).build()));
		when(productRepository.findById(10L)).thenReturn(Optional.of(product));

		// Act + Assert
		assertThrows(IllegalStateException.class, () -> orderService.placeOrder(dto));
	}

	@Test
	void shouldReturnSellerView_whenFindForSellerContainsMatchingItems() {
		// Arrange
		Long buyerId = 3L;
		Order order = Order.builder().id(900L).buyerId(buyerId).totalPrice(new BigDecimal("20.00")).build();
		when(orderRepository.findByBuyerIdWithItems(buyerId)).thenReturn(List.of(order));

		// Act
		List<Order> result = orderService.findByBuyer(buyerId);

		// Assert
		assertEquals(1, result.size());
		assertEquals(900L, result.getFirst().getId());
		assertEquals(buyerId, result.getFirst().getBuyerId());
	}
}

package com.example.minimarketplace.service;

import com.example.minimarketplace.dto.ProductRequestDTO;
import com.example.minimarketplace.dto.ProductResponseDTO;
import com.example.minimarketplace.exception.ResourceNotFoundException;
import com.example.minimarketplace.model.Product;
import com.example.minimarketplace.model.Role;
import com.example.minimarketplace.model.User;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private ProductService productService;

	@Test
	void shouldCreateProduct_whenSellerExists() {
		// Arrange
		ProductRequestDTO dto = new ProductRequestDTO("Coffee", "Arabica", new BigDecimal("9.99"), 10);
		User seller = User.builder().id(2L).name("Seller").email("seller@example.com").password("x").role(Role.SELLER).build();
		Product saved = Product.builder().id(100L).name("Coffee").description("Arabica").price(new BigDecimal("9.99")).quantity(10).sellerId(2L).build();
		when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
		when(productRepository.save(any(Product.class))).thenReturn(saved);

		// Act
		ProductResponseDTO result = productService.create(dto, 2L);

		// Assert
		assertEquals(100L, result.id());
		assertEquals("Seller", result.sellerName());
	}

	@Test
	void shouldThrowException_whenCreateSellerMissing() {
		// Arrange
		ProductRequestDTO dto = new ProductRequestDTO("Coffee", "Arabica", new BigDecimal("9.99"), 10);
		when(userRepository.findById(2L)).thenReturn(Optional.empty());

		// Act + Assert
		assertThrows(ResourceNotFoundException.class, () -> productService.create(dto, 2L));
	}

	@Test
	void shouldReturnUnknownSeller_whenFindByIdSellerMissing() {
		// Arrange
		Product product = Product.builder().id(11L).name("Tea").description("Green").price(new BigDecimal("4.50")).quantity(2).sellerId(8L).build();
		when(productRepository.findById(11L)).thenReturn(Optional.of(product));
		when(userRepository.findById(8L)).thenReturn(Optional.empty());

		// Act
		ProductResponseDTO result = productService.findById(11L);

		// Assert
		assertEquals("Unknown", result.sellerName());
	}

	@Test
	void shouldUpdateProduct_whenSellerOwnsProduct() {
		// Arrange
		Product existing = Product.builder().id(20L).name("Old").description("D").price(new BigDecimal("1.00")).quantity(1).sellerId(7L).build();
		ProductRequestDTO update = new ProductRequestDTO("New", "Desc", new BigDecimal("3.20"), 9);
		when(productRepository.findById(20L)).thenReturn(Optional.of(existing));
		when(productRepository.save(existing)).thenReturn(existing);
		when(userRepository.findById(7L)).thenReturn(Optional.of(User.builder().id(7L).name("Owner").build()));

		// Act
		ProductResponseDTO result = productService.update(20L, update, 7L);

		// Assert
		assertEquals("New", result.name());
		assertEquals(9, result.quantity());
	}

	@Test
	void shouldThrowException_whenUpdateByNonOwner() {
		// Arrange
		Product existing = Product.builder().id(21L).sellerId(7L).build();
		ProductRequestDTO update = new ProductRequestDTO("New", "Desc", new BigDecimal("3.20"), 9);
		when(productRepository.findById(21L)).thenReturn(Optional.of(existing));

		// Act + Assert
		assertThrows(IllegalArgumentException.class, () -> productService.update(21L, update, 8L));
		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void shouldThrowException_whenDeleteByNonOwner() {
		// Arrange
		Product existing = Product.builder().id(30L).sellerId(9L).build();
		when(productRepository.findById(30L)).thenReturn(Optional.of(existing));

		// Act + Assert
		assertThrows(IllegalArgumentException.class, () -> productService.delete(30L, 10L));
		verify(productRepository, never()).deleteById(any());
	}

	@Test
	void shouldReturnSellerProducts_whenFindBySellerCalled() {
		// Arrange
		Product p = Product.builder().id(31L).name("Milk").description("Fresh").price(new BigDecimal("2.00")).quantity(5).sellerId(9L).build();
		when(productRepository.findBySellerId(9L)).thenReturn(List.of(p));
		when(userRepository.findById(9L)).thenReturn(Optional.of(User.builder().id(9L).name("Seller Nine").build()));

		// Act
		List<ProductResponseDTO> result = productService.findBySeller(9L);

		// Assert
		assertEquals(1, result.size());
		assertEquals("Seller Nine", result.getFirst().sellerName());
	}
}

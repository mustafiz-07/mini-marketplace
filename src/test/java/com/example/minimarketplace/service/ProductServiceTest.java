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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        ProductRequestDTO dto = new ProductRequestDTO("Coffee", "Arabica", new BigDecimal("9.99"), 20);
        User seller = User.builder().id(5L).name("Seller Name").email("seller@example.com").password("x").role(Role.SELLER).build();
        Product savedProduct = Product.builder()
                .id(100L)
                .name("Coffee")
                .description("Arabica")
                .price(new BigDecimal("9.99"))
                .quantity(20)
                .sellerId(5L)
                .build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // Act
        ProductResponseDTO created = productService.create(dto, 5L);

        // Assert
        assertThat(created.id()).isEqualTo(100L);
        assertThat(created.name()).isEqualTo("Coffee");
        assertThat(created.sellerId()).isEqualTo(5L);
        assertThat(created.sellerName()).isEqualTo("Seller Name");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldThrowNotFound_whenCreateWithMissingSeller() {
        // Arrange
        ProductRequestDTO dto = new ProductRequestDTO("Coffee", "Arabica", new BigDecimal("9.99"), 20);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> productService.create(dto, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Seller not found with id: 99");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldReturnProduct_whenFindByIdExists() {
        // Arrange
        Product product = Product.builder()
                .id(101L)
                .name("Tea")
                .description("Green")
                .price(new BigDecimal("4.50"))
                .quantity(12)
                .sellerId(8L)
                .build();
        User seller = User.builder().id(8L).name("Sam").email("sam@example.com").password("x").role(Role.SELLER).build();

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(userRepository.findById(8L)).thenReturn(Optional.of(seller));

        // Act
        ProductResponseDTO found = productService.findById(101L);

        // Assert
        assertThat(found.id()).isEqualTo(101L);
        assertThat(found.sellerName()).isEqualTo("Sam");
    }

    @Test
    void shouldReturnUnknownSeller_whenSellerMissingDuringFindById() {
        // Arrange
        Product product = Product.builder()
                .id(102L)
                .name("Tea")
                .description("Green")
                .price(new BigDecimal("4.50"))
                .quantity(12)
                .sellerId(8L)
                .build();
        when(productRepository.findById(102L)).thenReturn(Optional.of(product));
        when(userRepository.findById(8L)).thenReturn(Optional.empty());

        // Act
        ProductResponseDTO found = productService.findById(102L);

        // Assert
        assertThat(found.sellerName()).isEqualTo("Unknown");
    }

    @Test
    void shouldThrowNotFound_whenFindByIdMissing() {
        // Arrange
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> productService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 404");
    }

    @Test
    void shouldReturnAvailableProducts_whenFindAvailableCalled() {
        // Arrange
        Product available = Product.builder()
                .id(103L)
                .name("Milk")
                .description("1L")
                .price(new BigDecimal("2.10"))
                .quantity(5)
                .sellerId(10L)
                .build();
        when(productRepository.findByQuantityGreaterThan(0)).thenReturn(List.of(available));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        // Act
        List<ProductResponseDTO> products = productService.findAvailable();

        // Assert
        assertThat(products).hasSize(1);
        assertThat(products.getFirst().name()).isEqualTo("Milk");
        assertThat(products.getFirst().sellerName()).isEqualTo("Unknown");
    }

    @Test
    void shouldReturnSearchResults_whenKeywordMatches() {
        // Arrange
        Product product = Product.builder()
                .id(104L)
                .name("Organic Honey")
                .description("500g")
                .price(new BigDecimal("12.00"))
                .quantity(3)
                .sellerId(11L)
                .build();
        User seller = User.builder().id(11L).name("Honey Farm").email("farm@example.com").password("x").role(Role.SELLER).build();

        when(productRepository.searchByKeyword("honey")).thenReturn(List.of(product));
        when(userRepository.findById(11L)).thenReturn(Optional.of(seller));

        // Act
        List<ProductResponseDTO> products = productService.search("honey");

        // Assert
        assertThat(products).hasSize(1);
        assertThat(products.getFirst().name()).isEqualTo("Organic Honey");
        assertThat(products.getFirst().sellerName()).isEqualTo("Honey Farm");
    }

    @Test
    void shouldUpdateProduct_whenSellerOwnsProduct() {
        // Arrange
        Product existing = Product.builder()
                .id(200L)
                .name("Old")
                .description("Old desc")
                .price(new BigDecimal("1.00"))
                .quantity(1)
                .sellerId(77L)
                .build();
        ProductRequestDTO updateDto = new ProductRequestDTO("New", "New desc", new BigDecimal("3.20"), 9);
        User seller = User.builder().id(77L).name("Owner").email("owner@example.com").password("x").role(Role.SELLER).build();

        when(productRepository.findById(200L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        when(userRepository.findById(77L)).thenReturn(Optional.of(seller));

        // Act
        ProductResponseDTO updated = productService.update(200L, updateDto, 77L);

        // Assert
        assertThat(updated.name()).isEqualTo("New");
        assertThat(updated.description()).isEqualTo("New desc");
        assertThat(updated.price()).isEqualByComparingTo("3.20");
        assertThat(updated.quantity()).isEqualTo(9);
    }

    @Test
    void shouldThrowException_whenUpdateByNonOwner() {
        // Arrange
        Product existing = Product.builder().id(201L).sellerId(77L).build();
        ProductRequestDTO updateDto = new ProductRequestDTO("New", "New desc", new BigDecimal("3.20"), 9);
        when(productRepository.findById(201L)).thenReturn(Optional.of(existing));

        // Act + Assert
        assertThatThrownBy(() -> productService.update(201L, updateDto, 88L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not authorized to update");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldDeleteProduct_whenSellerOwnsProduct() {
        // Arrange
        Product existing = Product.builder().id(300L).sellerId(55L).build();
        when(productRepository.findById(300L)).thenReturn(Optional.of(existing));

        // Act
        productService.delete(300L, 55L);

        // Assert
        verify(productRepository).deleteById(300L);
    }

    @Test
    void shouldThrowException_whenDeleteByNonOwner() {
        // Arrange
        Product existing = Product.builder().id(301L).sellerId(55L).build();
        when(productRepository.findById(301L)).thenReturn(Optional.of(existing));

        // Act + Assert
        assertThatThrownBy(() -> productService.delete(301L, 56L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not authorized to delete");

        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowNotFound_whenDeleteMissingProduct() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> productService.delete(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 999");
    }
}
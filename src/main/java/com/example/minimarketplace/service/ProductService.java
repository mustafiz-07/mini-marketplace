package com.example.minimarketplace.service;

import com.example.minimarketplace.dto.ProductRequestDTO;
import com.example.minimarketplace.dto.ProductResponseDTO;
import com.example.minimarketplace.exception.ResourceNotFoundException;
import com.example.minimarketplace.model.Product;
import com.example.minimarketplace.model.Role;
import com.example.minimarketplace.model.User;
import com.example.minimarketplace.repository.ProductRepository;
import com.example.minimarketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductResponseDTO create(ProductRequestDTO dto, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));
        if (seller.getRole() != Role.SELLER) {
            throw new IllegalArgumentException("Only users with SELLER role can create products.");
        }

        Product product = Product.builder()
                .name(dto.name())
                .description(dto.description())
                .price(dto.price())
                .quantity(dto.quantity())
                .sellerId(sellerId)
                .build();

        Product saved = productRepository.save(product);
        log.info("Created product '{}' by seller '{}'", saved.getName(), seller.getEmail());
        return toResponseDTO(saved, seller.getName());
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        String sellerName = userRepository.findById(product.getSellerId())
                .map(User::getName).orElse("Unknown");
        return toResponseDTO(product, sellerName);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> findAll() {
        return productRepository.findAll().stream()
                .map(p -> {
                    String sellerName = userRepository.findById(p.getSellerId())
                            .map(User::getName).orElse("Unknown");
                    return toResponseDTO(p, sellerName);
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> findAvailable() {
        return productRepository.findByQuantityGreaterThan(0).stream()
                .map(p -> {
                    String sellerName = userRepository.findById(p.getSellerId())
                            .map(User::getName).orElse("Unknown");
                    return toResponseDTO(p, sellerName);
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> findBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId).stream()
                .map(p -> {
                    String sellerName = userRepository.findById(p.getSellerId())
                            .map(User::getName).orElse("Unknown");
                    return toResponseDTO(p, sellerName);
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> search(String keyword) {
        return productRepository.searchByKeyword(keyword).stream()
                .map(p -> {
                    String sellerName = userRepository.findById(p.getSellerId())
                            .map(User::getName).orElse("Unknown");
                    return toResponseDTO(p, sellerName);
                }).toList();
    }

    public ProductResponseDTO update(Long id, ProductRequestDTO dto, Long sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (!product.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("You are not authorized to update this product.");
        }

        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        product.setQuantity(dto.quantity());

        Product updated = productRepository.save(product);
        String sellerName = userRepository.findById(sellerId).map(User::getName).orElse("Unknown");
        log.info("Updated product id={}", id);
        return toResponseDTO(updated, sellerName);
    }

    public void delete(Long id, Long sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        if (!product.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("You are not authorized to delete this product.");
        }
        productRepository.deleteById(id);
        log.info("Deleted product id={}", id);
    }

    private ProductResponseDTO toResponseDTO(Product p, String sellerName) {
        return new ProductResponseDTO(
                p.getId(), p.getName(), p.getDescription(),
                p.getPrice(), p.getQuantity(), p.getSellerId(), sellerName
        );
    }
}

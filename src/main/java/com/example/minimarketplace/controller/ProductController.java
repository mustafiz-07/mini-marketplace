package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.ProductRequestDTO;
import com.example.minimarketplace.dto.ProductResponseDTO;
import com.example.minimarketplace.service.ProductService;
import com.example.minimarketplace.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long sellerId) {

        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(productService.search(search));
        }
        if (sellerId != null) {
            return ResponseEntity.ok(productService.findBySeller(sellerId));
        }
        return ResponseEntity.ok(productService.findAvailable());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> create(
            @Valid @RequestBody ProductRequestDTO dto,
            @RequestParam(required = false) Long sellerId,
            Authentication authentication) {
        Long resolvedSellerId = resolveSellerId(authentication, sellerId);
        ProductResponseDTO created = productService.create(dto, resolvedSellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO dto,
            @RequestParam(required = false) Long sellerId,
            Authentication authentication) {
        Long resolvedSellerId = resolveSellerId(authentication, sellerId);
        return ResponseEntity.ok(productService.update(id, dto, resolvedSellerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam(required = false) Long sellerId,
            Authentication authentication) {
        Long resolvedSellerId = resolveSellerId(authentication, sellerId);
        productService.delete(id, resolvedSellerId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveSellerId(Authentication authentication, Long fallbackSellerId) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return userService.findByEmail(authentication.getName()).getId();
        }
        if (fallbackSellerId != null) {
            return fallbackSellerId;
        }
        throw new IllegalArgumentException("Seller identity is required.");
    }
}

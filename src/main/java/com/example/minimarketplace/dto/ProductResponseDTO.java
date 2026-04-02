package com.example.minimarketplace.dto;

import java.math.BigDecimal;

public record ProductResponseDTO(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        Long sellerId,
        String sellerName
) {}

package com.example.minimarketplace.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record OrderRequestDTO(

        @NotNull(message = "Buyer ID is required")
        Long buyerId,

        @NotNull(message = "Order items are required")
        @Size(min = 1, message = "Order must contain at least one item")
        List<OrderItemDTO> items
) {
    public record OrderItemDTO(
            @NotNull(message = "Product ID is required")
            Long productId,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity
    ) {}
}

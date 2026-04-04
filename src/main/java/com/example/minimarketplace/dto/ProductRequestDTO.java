package com.example.minimarketplace.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequestDTO(

        @NotBlank(message = "Product name is required")
        @Size(min = 2, max = 200)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be > 0")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull(message = "Quantity is required")
        @Min(value = 0)
        Integer quantity
) {}

package com.example.minimarketplace.dto;

import com.example.minimarketplace.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String tokenType;
    private Long userId;
    private Role role;
}

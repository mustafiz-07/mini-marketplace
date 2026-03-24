package com.example.minimarketplace.dto;

import com.example.minimarketplace.model.Role;
import com.example.minimarketplace.model.User;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserResponseDTO {
    Long id;
    String name;
    String email;
    Role role;

    public static UserResponseDTO from(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}

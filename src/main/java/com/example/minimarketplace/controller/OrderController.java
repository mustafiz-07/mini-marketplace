package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.OrderRequestDTO;
import com.example.minimarketplace.model.Role;
import com.example.minimarketplace.model.User;
import com.example.minimarketplace.model.Order;
import com.example.minimarketplace.service.OrderService;
import com.example.minimarketplace.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> placeOrder(
            @Valid @RequestBody OrderRequestDTO dto,
            Authentication authentication) {
        Long buyerId = resolveBuyerId(authentication, dto.buyerId());
        OrderRequestDTO normalized = new OrderRequestDTO(buyerId, dto.items());
        Order order = orderService.placeOrder(normalized);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "orderId",    order.getId(),
                "buyerId",    order.getBuyerId(),
                "orderDate",  order.getOrderDate().toString(),
                "totalPrice", order.getTotalPrice(),
                "itemCount",  order.getItems().size()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id, Authentication authentication) {
        Order order = orderService.findById(id);
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            User currentUser = userService.findByEmail(authentication.getName());
            if (currentUser.getRole() == Role.BUYER && !currentUser.getId().equals(order.getBuyerId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own orders.");
            }
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<Order>> getByBuyer(@PathVariable Long buyerId, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            User currentUser = userService.findByEmail(authentication.getName());
            if (currentUser.getRole() == Role.BUYER && !currentUser.getId().equals(buyerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own orders.");
            }
        }
        return ResponseEntity.ok(orderService.findByBuyer(buyerId));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAll(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            User currentUser = userService.findByEmail(authentication.getName());
            if (currentUser.getRole() == Role.BUYER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buyers cannot access all orders.");
            }
        }
        return ResponseEntity.ok(orderService.findAll());
    }

    private Long resolveBuyerId(Authentication authentication, Long fallbackBuyerId) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return userService.findByEmail(authentication.getName()).getId();
        }
        return fallbackBuyerId;
    }
}

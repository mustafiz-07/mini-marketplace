package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.OrderRequestDTO;
import com.example.minimarketplace.model.Order;
import com.example.minimarketplace.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> placeOrder(
            @Valid @RequestBody OrderRequestDTO dto) {
        Order order = orderService.placeOrder(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "orderId",    order.getId(),
                "buyerId",    order.getBuyerId(),
                "orderDate",  order.getOrderDate().toString(),
                "totalPrice", order.getTotalPrice(),
                "itemCount",  order.getItems().size()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<Order>> getByBuyer(@PathVariable Long buyerId) {
        return ResponseEntity.ok(orderService.findByBuyer(buyerId));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAll() {
        return ResponseEntity.ok(orderService.findAll());
    }
}

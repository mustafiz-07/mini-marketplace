package com.example.minimarketplace.service;

import com.example.minimarketplace.dto.OrderRequestDTO;
import com.example.minimarketplace.exception.ResourceNotFoundException;
import com.example.minimarketplace.model.Order;
import com.example.minimarketplace.model.OrderItem;
import com.example.minimarketplace.model.Product;
import com.example.minimarketplace.repository.OrderRepository;
import com.example.minimarketplace.repository.ProductRepository;
import com.example.minimarketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Order placeOrder(OrderRequestDTO dto) {
        // Validate buyer exists
        userRepository.findById(dto.buyerId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found with id: " + dto.buyerId()));

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        // Build items and compute total
        for (OrderRequestDTO.OrderItemDTO itemDTO : dto.items()) {
            Product product = productRepository.findById(itemDTO.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + itemDTO.productId()));

            if (product.getQuantity() < itemDTO.quantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getQuantity() +
                        ", Requested: " + itemDTO.quantity());
            }

            // Deduct stock
            product.setQuantity(product.getQuantity() - itemDTO.quantity());
            productRepository.save(product);

            BigDecimal lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemDTO.quantity()));
            totalPrice = totalPrice.add(lineTotal);

            items.add(OrderItem.builder()
                    .productId(itemDTO.productId())
                    .quantity(itemDTO.quantity())
                    .build());
        }

        Order order = Order.builder()
                .buyerId(dto.buyerId())
                .orderDate(LocalDateTime.now())
                .totalPrice(totalPrice)
                .items(new ArrayList<>())
                .build();

        // Link items to order
        for (OrderItem item : items) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);
        log.info("Order #{} placed by buyer {} | Total: {}", saved.getId(), dto.buyerId(), totalPrice);
        return saved;
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> findByBuyer(Long buyerId) {
        return orderRepository.findByBuyerIdWithItems(buyerId);
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SellerOrderView> findForSeller(Long sellerId) {
        List<Order> orders = orderRepository.findOrdersForSellerWithItems(sellerId);

        Set<Long> buyerIds = new HashSet<>();
        Set<Long> productIds = new HashSet<>();
        for (Order order : orders) {
            buyerIds.add(order.getBuyerId());
            for (OrderItem item : order.getItems()) {
                productIds.add(item.getProductId());
            }
        }

        Map<Long, String> buyerNameById = new HashMap<>();
        Map<Long, String> buyerEmailById = new HashMap<>();
        userRepository.findAllById(buyerIds).forEach(user -> {
            buyerNameById.put(user.getId(), user.getName());
            buyerEmailById.put(user.getId(), user.getEmail());
        });

        Map<Long, Product> productById = new HashMap<>();
        productRepository.findAllById(productIds).forEach(product -> productById.put(product.getId(), product));

        List<SellerOrderView> result = new ArrayList<>();
        for (Order order : orders) {
            List<SellerOrderLineItem> sellerItems = new ArrayList<>();
            BigDecimal sellerTotal = BigDecimal.ZERO;

            for (OrderItem item : order.getItems()) {
                Product product = productById.get(item.getProductId());
                if (product == null || !product.getSellerId().equals(sellerId)) {
                    continue;
                }

                BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                sellerTotal = sellerTotal.add(lineTotal);

                sellerItems.add(new SellerOrderLineItem(
                        item.getProductId(),
                        product.getName(),
                        item.getQuantity(),
                        product.getPrice(),
                        lineTotal
                ));
            }

            if (!sellerItems.isEmpty()) {
                result.add(new SellerOrderView(
                        order.getId(),
                        order.getBuyerId(),
                        buyerNameById.getOrDefault(order.getBuyerId(), "Unknown"),
                        buyerEmailById.getOrDefault(order.getBuyerId(), "-"),
                        order.getOrderDate(),
                        sellerTotal,
                        sellerItems
                ));
            }
        }

        return result;
    }

    public record SellerOrderLineItem(
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}

    public record SellerOrderView(
            Long orderId,
            Long buyerId,
            String buyerName,
            String buyerEmail,
            LocalDateTime orderDate,
            BigDecimal sellerTotal,
            List<SellerOrderLineItem> items
    ) {}
}

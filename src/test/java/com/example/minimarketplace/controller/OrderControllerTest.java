package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.OrderRequestDTO;
import com.example.minimarketplace.exception.GlobalExceptionHandler;
import com.example.minimarketplace.model.Order;
import com.example.minimarketplace.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldPlaceOrder_whenValidRequest() throws Exception {
        OrderRequestDTO request = new OrderRequestDTO(1L, List.of(new OrderRequestDTO.OrderItemDTO(10L, 2)));
        Order order = Order.builder()
                .id(101L)
                .buyerId(1L)
                .orderDate(LocalDateTime.of(2026, 4, 3, 10, 0))
                .totalPrice(new BigDecimal("11.00"))
                .items(List.of())
                .build();
        when(orderService.placeOrder(request)).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(101L))
                .andExpect(jsonPath("$.buyerId").value(1L))
                .andExpect(jsonPath("$.itemCount").value(0));
    }

    @Test
    void shouldReturnOrderById_whenFound() throws Exception {
        Order order = Order.builder()
                .id(201L)
                .buyerId(2L)
                .orderDate(LocalDateTime.of(2026, 4, 3, 11, 0))
                .totalPrice(new BigDecimal("9.50"))
                .items(List.of())
                .build();
        when(orderService.findById(201L)).thenReturn(order);

        mockMvc.perform(get("/api/orders/{id}", 201L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(201L))
                .andExpect(jsonPath("$.buyerId").value(2L));
    }

    @Test
    void shouldReturnBuyerOrders_whenBuyerHasOrders() throws Exception {
        Order order = Order.builder()
                .id(301L)
                .buyerId(3L)
                .orderDate(LocalDateTime.of(2026, 4, 3, 12, 0))
                .totalPrice(new BigDecimal("19.00"))
                .items(List.of())
                .build();
        when(orderService.findByBuyer(3L)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/buyer/{buyerId}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(301L))
                .andExpect(jsonPath("$[0].buyerId").value(3L));
    }

    @Test
    void shouldReturnAllOrders_whenRequested() throws Exception {
        Order first = Order.builder()
                .id(401L)
                .buyerId(4L)
                .orderDate(LocalDateTime.of(2026, 4, 3, 13, 0))
                .totalPrice(new BigDecimal("4.00"))
                .items(List.of())
                .build();
        Order second = Order.builder()
                .id(402L)
                .buyerId(5L)
                .orderDate(LocalDateTime.of(2026, 4, 3, 13, 30))
                .totalPrice(new BigDecimal("7.00"))
                .items(List.of())
                .build();
        when(orderService.findAll()).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(401L))
                .andExpect(jsonPath("$[1].id").value(402L));
    }
}

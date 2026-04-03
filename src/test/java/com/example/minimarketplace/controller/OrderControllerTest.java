package com.example.minimarketplace.controller;

import com.example.minimarketplace.dto.OrderRequestDTO;
import com.example.minimarketplace.exception.GlobalExceptionHandler;
import com.example.minimarketplace.exception.ResourceNotFoundException;
import com.example.minimarketplace.model.Order;
import com.example.minimarketplace.model.OrderItem;
import com.example.minimarketplace.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
        // Arrange
        OrderRequestDTO request = new OrderRequestDTO(1L, List.of(new OrderRequestDTO.OrderItemDTO(10L, 2)));
        Order placed = Order.builder()
                .id(100L)
                .buyerId(1L)
                .orderDate(LocalDateTime.of(2026, 4, 3, 10, 0, 0))
                .totalPrice(new BigDecimal("11.00"))
                .items(List.of(OrderItem.builder().id(1L).productId(10L).quantity(2).build()))
                .build();
        when(orderService.placeOrder(request)).thenReturn(placed);

        // Act + Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(100L))
                .andExpect(jsonPath("$.buyerId").value(1L))
                .andExpect(jsonPath("$.totalPrice").value(11.00))
                .andExpect(jsonPath("$.itemCount").value(1));
    }

    @Test
    void shouldReturnBadRequest_whenPlaceOrderRequestInvalid() throws Exception {
        // Arrange
        OrderRequestDTO invalid = new OrderRequestDTO(null, List.of());

        // Act + Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.buyerId").exists())
                .andExpect(jsonPath("$.items").exists());
    }

    @Test
    void shouldReturnOrderById_whenOrderExists() throws Exception {
        // Arrange
        Order order = Order.builder()
                .id(101L)
                .buyerId(2L)
                .orderDate(LocalDateTime.of(2026, 4, 3, 10, 30, 0))
                .totalPrice(new BigDecimal("5.00"))
                .items(List.of())
                .build();
        when(orderService.findById(101L)).thenReturn(order);

        // Act + Assert
        mockMvc.perform(get("/api/orders/{id}", 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.buyerId").value(2L));
    }

    @Test
    void shouldReturnNotFound_whenOrderMissing() throws Exception {
        // Arrange
        when(orderService.findById(404L)).thenThrow(new ResourceNotFoundException("Order not found with id: 404"));

        // Act + Assert
        mockMvc.perform(get("/api/orders/{id}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Order not found with id: 404"));
    }

    @Test
    void shouldReturnOrdersByBuyer_whenBuyerHasOrders() throws Exception {
        // Arrange
        Order order = Order.builder()
                .id(201L)
                .buyerId(7L)
                .orderDate(LocalDateTime.of(2026, 4, 3, 11, 0, 0))
                .totalPrice(new BigDecimal("9.90"))
                .items(List.of())
                .build();
        when(orderService.findByBuyer(7L)).thenReturn(List.of(order));

        // Act + Assert
        mockMvc.perform(get("/api/orders/buyer/{buyerId}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(201L))
                .andExpect(jsonPath("$[0].buyerId").value(7L));
    }

    @Test
    void shouldReturnAllOrders_whenRequestValid() throws Exception {
        // Arrange
        when(orderService.findAll()).thenReturn(List.of(
                Order.builder().id(1L).buyerId(1L).orderDate(LocalDateTime.of(2026, 4, 3, 8, 0, 0)).totalPrice(new BigDecimal("1.00")).items(List.of()).build(),
                Order.builder().id(2L).buyerId(2L).orderDate(LocalDateTime.of(2026, 4, 3, 9, 0, 0)).totalPrice(new BigDecimal("2.00")).items(List.of()).build()
        ));

        // Act + Assert
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }
}
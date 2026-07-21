package com.example.tds.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.tds.enums.OrderStatus;
import org.springframework.http.HttpStatus;
import com.example.tds.service.OrderService;
import org.springframework.http.ResponseEntity;
import com.example.tds.dto.responses.ApiResponse;
import org.springframework.web.bind.annotation.*;
import com.example.tds.dto.responses.OrderResponse;
import com.example.tds.dto.requests.orders.OrderDeliveredRequest;
import com.example.tds.dto.responses.OrderForDeliveryAgentResponse;

import java.util.Map;
import java.util.UUID;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery-agents")
public class DeliveryAgentOrderController {
    private final OrderService orderService;

    @GetMapping("/{agentId}/orders/{orderId}")
    public ResponseEntity<ApiResponse> getOrder(
            @PathVariable("agentId") UUID agentId,
            @PathVariable("orderId") UUID orderId) {
        
        OrderForDeliveryAgentResponse response = orderService.getOrderForDeliveryAgent(agentId, orderId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Order retrieved successfully")
                        .success(true)
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/{agentId}/orders")
    public ResponseEntity<ApiResponse> getOrders(
            @PathVariable("agentId") UUID agentId,
            @RequestParam(value = "active", required = false, defaultValue = "false") Boolean active
    ){
        List<OrderForDeliveryAgentResponse> orders = orderService.getOrdersForDeliveryAgent(agentId, active);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Orders found successfully")
                        .success(true)
                        .data(Map.of("orders", orders))
                        .build()
        );
    }

    @PatchMapping("/{agentId}/orders/{orderId}/status/pickup")
    public ResponseEntity<ApiResponse> updateOrderStatusToPickup(
            @PathVariable("agentId") UUID agentId,
            @PathVariable("orderId") UUID orderId) {

        OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.PICKED_UP);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Order status updated successfully")
                        .success(true)
                        .data(Map.of("order", response))
                        .build()
        );
    }

    @PatchMapping("/{agentId}/orders/{orderId}/status/delivered")
    public ResponseEntity<ApiResponse> handleOrderDelivered(
            @PathVariable("agentId") UUID agentId,
            @PathVariable("orderId") UUID orderId,
            @RequestBody @Valid OrderDeliveredRequest request) {

        OrderResponse response = orderService.handleOrderDelivery(orderId, request);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Order status updated successfully")
                        .success(true)
                        .data(Map.of("order", response))
                        .build()
        );
    }
}

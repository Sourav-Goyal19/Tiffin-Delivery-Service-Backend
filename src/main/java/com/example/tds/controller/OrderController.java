package com.example.tds.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.example.tds.service.OrderService;
import org.springframework.http.ResponseEntity;
import com.example.tds.dto.responses.ApiResponse;
import com.example.tds.dto.responses.OrderResponse;
import com.example.tds.dto.responses.OrderForChefResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.example.tds.dto.responses.DeliveryAgentResponse;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.tds.dto.requests.orders.UpdateOrderStatusRequest;
import com.example.tds.dto.requests.orders.UpdateMultipleOrdersStatusRequest;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chefs")
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/{chefId}/orders/today")
    public ResponseEntity<ApiResponse> getOrdersForChef(@PathVariable("chefId") UUID chefId) {
        OrderForChefResponse response = orderService.getOrdersForChef(chefId);

        Map<String, Object> data = new HashMap<>();
        data.put("orders", response.getOrders());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Orders retrieved successfully")
                        .success(true)
                        .data(data)
                        .build()
        );
    }

    @PatchMapping("/{chefId}/orders/{orderId}/status")
    public ResponseEntity<ApiResponse> updateOrderStatus(
            @PathVariable("chefId") UUID chefId,
            @PathVariable("orderId") UUID orderId,
            @RequestBody @Valid UpdateOrderStatusRequest request) {

        OrderResponse response = orderService.updateOrderStatus(orderId, request.getStatus());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Order status updated successfully")
                        .success(true)
                        .data(Map.of("order", response))
                        .build()
        );
    }

    @PatchMapping("/{chefId}/orders/status")
    public ResponseEntity<ApiResponse> updateMultipleOrdersStatus(
            @PathVariable("chefId") UUID chefId,
            @RequestBody @Valid UpdateMultipleOrdersStatusRequest request) {

        List<OrderResponse> response = orderService.updateMultipleOrdersStatus(request.getOrderIds(), request.getStatus());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Orders status updated successfully")
                        .success(true)
                        .data(Map.of("orders", response))
                        .build()
        );
    }
    @PatchMapping("/{chefId}/orders/{orderId}/assign")
    public ResponseEntity<ApiResponse> assignDeliveryAgent(
            @PathVariable("chefId") UUID chefId,
            @PathVariable("orderId") UUID orderId) {

        DeliveryAgentResponse response = orderService.assignDeliveryAgent(orderId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Delivery agent assigned successfully")
                        .success(true)
                        .data(Map.of("deliveryAgent", response))
                        .build()
        );
    }
}

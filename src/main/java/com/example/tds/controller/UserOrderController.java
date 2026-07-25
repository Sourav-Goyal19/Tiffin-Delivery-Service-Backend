package com.example.tds.controller;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.example.tds.service.OrderService;
import com.example.tds.service.DeliveryAgentService;
import com.example.tds.dto.responses.GoogleMapsRouteResponse;
import org.springframework.http.ResponseEntity;
import com.example.tds.dto.responses.ApiResponse;
import org.springframework.web.bind.annotation.*;
import com.example.tds.dto.responses.OrderForUserResponse;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserOrderController {
    private final OrderService orderService;
    private final DeliveryAgentService deliveryAgentService;

    @GetMapping("/{userId}/orders/{orderId}/route")
    public ResponseEntity<ApiResponse> getOrderRoute(
            @PathVariable("userId") UUID userId,
            @PathVariable("orderId") UUID orderId) {

        // Optionally, verify that the order belongs to the user
        // orderService.getOrderForUser(userId, orderId); // this will throw if not found/unauthorized

        GoogleMapsRouteResponse route = deliveryAgentService.getRouteForOrder(orderId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Route fetched successfully")
                        .success(true)
                        .data(Map.of("route", route))
                        .build()
        );
    }

    @GetMapping("/{userId}/orders/{orderId}")
    public ResponseEntity<ApiResponse> getOrder(
            @PathVariable("userId") UUID userId,
            @PathVariable("orderId") UUID orderId) {

        OrderForUserResponse response = orderService.getOrderForUser(userId, orderId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Order retrieved successfully")
                        .success(true)
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/{userId}/orders")
    public ResponseEntity<ApiResponse> getOrders(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date
    ){
        List<OrderForUserResponse> orders = orderService.getOrdersForUser(userId, date);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Orders found successfully")
                        .success(true)
                        .data(Map.of("orders", orders))
                        .build()
        );
    }
}

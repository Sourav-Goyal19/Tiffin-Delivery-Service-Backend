package com.example.tds.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.tds.dto.responses.ApiResponse;
import org.springframework.web.bind.annotation.*;
import com.example.tds.service.SubscriptionService;
import com.example.tds.dto.responses.SubscriptionResponse;
import com.example.tds.dto.responses.SubscriptionWithDetailsResponse;
import com.example.tds.dto.requests.subscriptions.CreateSubscriptionRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    @PostMapping("/users/{userId}/subscriptions")
    public ResponseEntity<ApiResponse> handleSubscriptionCreation(@PathVariable("userId") UUID userId, @RequestBody @Valid CreateSubscriptionRequest request){
        Map<String, Object> response = subscriptionService.createSubscription(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.builder()
                        .message("Subscription created successfully")
                        .success(true)
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/users/{userId}/subscriptions")
    public ResponseEntity<ApiResponse> getSubscriptions(@PathVariable("userId") UUID userId){
        List<SubscriptionWithDetailsResponse> response = subscriptionService.getSubscriptionsByUserId(userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Subscriptions found successfully")
                        .success(true)
                        .data(Map.of("subscriptions", response))
                        .build()
        );
    }

    @GetMapping("/users/{userId}/subscriptions/{subscriptionId}")
    public ResponseEntity<ApiResponse> getSubscriptionById(@PathVariable("subscriptionId") UUID subscriptionId){
        SubscriptionWithDetailsResponse response = subscriptionService.getSubscription(subscriptionId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Subscription found successfully")
                        .success(true)
                        .data(Map.of("subscription", response))
                        .build()
        );
    }

    @GetMapping("/chefs/{chefId}/subscriptions")
    public ResponseEntity<ApiResponse> getChefSubscriptions(@PathVariable("chefId") UUID chefId){
        List<SubscriptionWithDetailsResponse> response = subscriptionService.getSubscriptionsByChefId(chefId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Subscriptions found successfully")
                        .success(true)
                        .data(Map.of("subscriptions", response))
                        .build()
        );
    }

    @GetMapping("/chefs/{chefId}/subscriptions/{subscriptionId}")
    public ResponseEntity<ApiResponse> getSubscriptionByIdForChef(@PathVariable("subscriptionId") UUID subscriptionId){
        SubscriptionWithDetailsResponse response = subscriptionService.getSubscription(subscriptionId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Subscription found successfully")
                        .success(true)
                        .data(Map.of("subscription", response))
                        .build()
        );
    }
}

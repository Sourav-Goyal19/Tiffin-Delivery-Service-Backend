package com.example.tds.controller;

import com.example.tds.dto.responses.ApiResponse;
import com.example.tds.entity.UserEntity;
import com.example.tds.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/users/{userId}/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/fee/{chefId}")
    public ResponseEntity<ApiResponse> getDeliveryFee(@PathVariable("userId") UUID userId, @PathVariable("chefId") UUID chefId){
        double fee = deliveryService.calculateDeliveryFee(userId, chefId);

        Map<String, Object> data = Map.of("deliveryFee", fee);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Delivery fee calculated successfully")
                        .success(true)
                        .data(data)
                        .build()
        );
    }
}

package com.example.tds.controller;

import com.example.tds.dto.requests.common.*;
import com.example.tds.dto.responses.ApiResponse;
import com.example.tds.dto.responses.DeliveryAgentResponse;
import com.example.tds.entity.DeliveryAgentEntity;
import com.example.tds.service.DeliveryAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery-agents")
public class DeliveryAgentController {
    private final DeliveryAgentService deliveryAgentService;

    @PatchMapping("/{deliveryAgentId}/name")
    public ResponseEntity<ApiResponse> updateAgentName(@PathVariable("deliveryAgentId") UUID deliveryAgentId, @RequestBody @Valid UpdateNameRequest updateNameRequest) {
        DeliveryAgentResponse response = deliveryAgentService.handleUpdateName(deliveryAgentId, updateNameRequest);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Name updated successfully")
                        .success(true)
                        .data(Map.of("deliveryAgent", response))
                        .build()
        );
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody @Valid OtpVerifyRequest verifyRequest) {
        DeliveryAgentResponse response = deliveryAgentService.handleOtpVerification(verifyRequest);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("OTP verified successfully")
                        .data(Map.of("deliveryAgent", response))
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> handleGetAgent(@RequestAttribute("deliveryAgent") DeliveryAgentEntity agentEntity) {
        DeliveryAgentResponse response = deliveryAgentService.handleGetAgent(agentEntity);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Delivery Agent found successfully")
                        .success(true)
                        .data(Map.of("deliveryAgent", response))
                        .build()
        );
    }

    @PostMapping("/otp/generate")
    public ResponseEntity<ApiResponse> generateOtp(@RequestBody @Valid OtpGenerationRequest generationRequest) {
        deliveryAgentService.handleOtpGeneration(generationRequest);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("OTP generated successfully")
                        .success(true)
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> updateRefreshToken(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        DeliveryAgentResponse response = deliveryAgentService.handleRefresh(refreshTokenRequest.getRefreshToken());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Token updated successfully")
                        .success(true)
                        .data(Map.of(
                                "accessToken", response.getAccessToken(),
                                "refreshToken", response.getRefreshToken()
                        ))
                        .build()
        );
    }
}

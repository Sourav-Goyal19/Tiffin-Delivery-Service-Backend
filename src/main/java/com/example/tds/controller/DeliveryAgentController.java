package com.example.tds.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.example.tds.dto.requests.common.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.tds.dto.responses.ApiResponse;
import com.example.tds.entity.DeliveryAgentEntity;
import com.example.tds.service.DeliveryAgentService;
import com.example.tds.service.LogoutService;
import com.example.tds.dto.responses.DeliveryAgentResponse;
import com.example.tds.dto.responses.GoogleMapsRouteResponse;

import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery-agents")
public class DeliveryAgentController {
    private final DeliveryAgentService deliveryAgentService;
    private final LogoutService logoutService;

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

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> handleLogout(
            @RequestAttribute("deliveryAgent") DeliveryAgentEntity currentAgent,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody(required = false) @Valid LogoutRequest logoutRequest
    ) {
        String rawAccessToken = authorizationHeader.substring("Bearer ".length());
        UUID actorId = currentAgent.getDeliveryAgentId();
        logoutService.revokeAccessToken(rawAccessToken, "deliveryAgent", actorId);
        if (logoutRequest != null) {
            logoutService.revokeRefreshToken(logoutRequest.getRefreshToken(), "deliveryAgent", actorId);
        }
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Logged out successfully")
                        .success(true)
                        .build()
        );
    }

    @PatchMapping("/{deliveryAgentId}/fcm-token")
    public ResponseEntity<ApiResponse> updateFcmToken(
            @PathVariable("deliveryAgentId") UUID deliveryAgentId,
            @RequestBody @Valid UpdateFcmTokenRequest request
    ) {
        deliveryAgentService.handleUpdateFcmToken(deliveryAgentId, request.getFcmToken());

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("FCM token updated successfully")
                        .success(true)
                        .build()
        );
    }

    @PatchMapping("/{deliveryAgentId}/delivery-request/{orderId}/accept")
    public ResponseEntity<ApiResponse> updateDeliveryRequest(
            @PathVariable("deliveryAgentId") UUID deliveryAgentId,
            @PathVariable("orderId") UUID orderId
    ){
        deliveryAgentService.publishDeliveryRequest(deliveryAgentId, orderId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Published successfully")
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/route/{orderId}")
    public ResponseEntity<ApiResponse> getRoute(@PathVariable("orderId") UUID orderId) {
        GoogleMapsRouteResponse route = deliveryAgentService.getRouteForOrder(orderId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Route fetched successfully")
                        .success(true)
                        .data(Map.of("route", route))
                        .build()
        );
    }
}
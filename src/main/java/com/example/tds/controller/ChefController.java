package com.example.tds.controller;

import com.example.tds.dto.requests.common.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.tds.entity.ChefEntity;
import com.example.tds.service.ChefService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.tds.dto.responses.ApiResponse;
import com.example.tds.dto.responses.ChefResponse;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chefs")
public class ChefController {
    private final ChefService chefService;

    @PatchMapping("/{chefId}/name")
    public ResponseEntity<ApiResponse> updateChefName(@PathVariable("chefId") UUID chefId, @RequestBody @Valid UpdateNameRequest updateNameRequest) {
        ChefResponse chefResponse = chefService.handleUpdateName(chefId, updateNameRequest);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("name updated successfully")
                        .success(true)
                        .data(Map.of("chef", chefResponse))
                        .build()
        );
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody @Valid OtpVerifyRequest verifyRequest){
        ChefResponse response= chefService.handleOtpVerification(verifyRequest);

        Map<String, Object> data = Map.of("chef", response);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.builder()
                        .message("OTP verified successfully")
                        .data(data)
                        .success(true)
                        .build()
                );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> handleGetChef(@RequestAttribute("chef")ChefEntity chefEntity){
        ChefResponse response = chefService.handleGetChef(chefEntity);

        Map<String, Object> data = Map.of("chef", response);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Chef found successfully")
                        .success(true)
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/otp/generate")
    public ResponseEntity<ApiResponse> generateOtp(@RequestBody @Valid OtpGenerationRequest generationRequest){
        chefService.handleOtpGeneration(generationRequest);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("OTP generated successfully")
                        .success(true)
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> updateRefreshToken(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest){
        ChefResponse response = chefService.handleRefresh(refreshTokenRequest.getRefreshToken());

        Map<String, Object> data = Map.of(
                "accessToken", response.getAccessToken(),
                "refreshToken", response.getRefreshToken()
        );

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.builder()
                        .message("Token updated successfully")
                        .success(true)
                        .data(data)
                        .build()
                );
    }

    @PatchMapping("/{chefId}/location")
    public ResponseEntity<ApiResponse> updateLocation(@PathVariable("chefId") UUID chefId, @RequestBody @Valid UpdateLocationRequest locationRequest){
        ChefResponse response = chefService.handleUpdateLocation(chefId, locationRequest);

        Map<String, Object> data = Map.of("chef", response);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Location updated successfully")
                        .success(true)
                        .data(data)
                        .build()
        );
    }
}

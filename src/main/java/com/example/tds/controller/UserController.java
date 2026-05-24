package com.example.tds.controller;

import com.example.tds.dto.requests.RefreshTokenRequest;
import com.example.tds.dto.requests.UpdateLocationRequest;
import com.example.tds.dto.requests.OtpGenerationRequest;
import com.example.tds.dto.requests.OtpVerifyRequest;
import com.example.tds.dto.requests.users.UserNameUpdateRequest;
import com.example.tds.dto.requests.users.UserSignUpRequest;
import com.example.tds.dto.responses.ApiResponse;
import com.example.tds.dto.responses.UserResponse;
import com.example.tds.entity.UserEntity;
import com.example.tds.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PatchMapping("/{userId}/name")
    public ResponseEntity<ApiResponse> updateUserName(@PathVariable("userId") UUID userId, @RequestBody @Valid UserNameUpdateRequest nameUpdateRequest){
        UserResponse response = userService.handleUpdateName(userId, nameUpdateRequest);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("name update successfully")
                        .success(true)
                        .data(Map.of("user", response))
                        .build()
        );
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse> handleOtpVerification(@RequestBody @Valid OtpVerifyRequest verifyRequest){
        UserResponse response= userService.handleOtpVerification(verifyRequest);

        Map<String, Object> data = Map.of("user", response);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.builder()
                        .message("OTP verified successfully")
                        .data(data)
                        .success(true)
                        .build()
        );
    }

    @PostMapping("/otp/generate")
    public ResponseEntity<ApiResponse> handleOtpGeneration(@RequestBody @Valid OtpGenerationRequest generationRequest){
        userService.handleOtpGeneration(generationRequest);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.builder()
                        .message("OTP generated successfully")
                        .success(true)
                        .build()
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> handleGetUser(@RequestAttribute("user") UserEntity currentUser){
        UserResponse response = userService.handleGetUser(currentUser);

        Map<String, Object> data = Map.of("user", response);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("User found successfully")
                        .success(true)
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> updateRefreshToken(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest){
        UserResponse response = userService.handleRefresh(refreshTokenRequest.getRefreshToken());

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

    @PatchMapping("/{userId}/location")
    public ResponseEntity<ApiResponse> updateLocation(@PathVariable("userId") UUID userId, @RequestBody @Valid UpdateLocationRequest locationRequest){
        UserResponse response = userService.handleUpdateLocation(userId, locationRequest);

        Map<String, Object> data = Map.of("user", response);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Location updated successfully")
                        .success(true)
                        .data(data)
                        .build()
        );
    }
}

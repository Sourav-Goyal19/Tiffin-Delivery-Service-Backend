package com.example.tds.dto.requests.common;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateFcmTokenRequest {
    @NotBlank(message = "FCM token is required")
    private String fcmToken;
}

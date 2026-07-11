package com.example.tds.dto.responses;

import com.example.tds.enums.DeliveryAgentCurrentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryAgentResponse {
    private UUID deliveryAgentId;
    private String name;
    private String mobileNo;
    private String fcmToken;
    private DeliveryAgentCurrentStatus currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;

    private String refreshToken;
    private String accessToken;
}

package com.example.tds.dto.requests.orders;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderDeliveredRequest {
    @NotNull(message = "Order's otp is required")
    private Integer orderOtp;
}

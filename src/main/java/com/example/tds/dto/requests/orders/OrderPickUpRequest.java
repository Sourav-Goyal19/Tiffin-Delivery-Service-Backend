package com.example.tds.dto.requests.orders;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderPickUpRequest {
    @NotNull(message = "Order's pick-up otp is required")
    private Integer pickUpOtp;
}

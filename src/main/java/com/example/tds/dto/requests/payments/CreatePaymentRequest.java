package com.example.tds.dto.requests.payments;

import com.example.tds.enums.PaymentVia;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreatePaymentRequest {
    @NotNull(message = "Razorpay Payment Id is required")
    private String razorpayPaymentId;

    @NotNull(message = "A valid bill id is required")
    private UUID billId;

    @NotNull(message = "A proper payment via is required")
    private PaymentVia paymentVia;
}

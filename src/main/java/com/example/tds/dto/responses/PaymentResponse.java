package com.example.tds.dto.responses;

import com.example.tds.enums.PaymentVia;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private UUID paymentId;
    private UUID billId;
    private Double amount;
    private PaymentVia paymentVia;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

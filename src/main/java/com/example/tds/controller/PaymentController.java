package com.example.tds.controller;

import com.example.tds.dto.requests.payments.CreatePaymentRequest;
import com.example.tds.dto.responses.ApiResponse;
import com.example.tds.dto.responses.PaymentResponse;
import com.example.tds.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse> createPayment(
            @RequestBody @Valid CreatePaymentRequest createPaymentRequest
    ) {
        PaymentResponse response = paymentService.createPayment(createPaymentRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.builder()
                        .message("Payment created successfully")
                        .success(true)
                        .data(Map.of("payment", response))
                        .build()
        );
    }

    @GetMapping("/bills/{billId}")
    public ResponseEntity<ApiResponse> getPaymentByBillId(
            @PathVariable("billId") UUID billId
    ) {
        PaymentResponse response = paymentService.getPaymentByBillId(billId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Payment found successfully")
                        .success(true)
                        .data(Map.of("payment", response))
                        .build()
        );
    }
}

package com.example.tds.mapper;

import com.example.tds.dto.requests.payments.CreatePaymentRequest;
import com.example.tds.dto.responses.PaymentResponse;
import com.example.tds.entity.PaymentEntity;
import com.razorpay.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface PaymentMapper {
    PaymentEntity toPaymentEntity(CreatePaymentRequest request);
    PaymentResponse toPaymentResponse(PaymentEntity payment);
}

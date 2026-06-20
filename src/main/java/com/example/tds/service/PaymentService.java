package com.example.tds.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.tds.entity.BillEntity;
import com.example.tds.entity.PaymentEntity;
import com.example.tds.enums.PaymentStatus;
import com.example.tds.mapper.PaymentMapper;
import org.springframework.stereotype.Service;
import com.example.tds.entity.SubscriptionEntity;
import com.example.tds.repository.BillRepository;
import com.example.tds.repository.PaymentRepository;
import com.example.tds.dto.responses.PaymentResponse;
import com.example.tds.repository.SubscriptionRepository;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.dto.requests.payments.CreatePaymentRequest;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final BillRepository billRepository;
    private final SubscriptionRepository subscriptionRepository;

    public PaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
        UUID billId = createPaymentRequest.getBillId();

        BillEntity bill = billRepository.findByBillId(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));

        SubscriptionEntity subscription = bill.getSubscription();

        PaymentEntity paymentEntity = new PaymentEntity();

        paymentEntity.setRazorpayPaymentId(createPaymentRequest.getRazorpayPaymentId());
        paymentEntity.setBill(bill);
        paymentEntity.setAmount(bill.getAmount());
        paymentEntity.setPaymentVia(createPaymentRequest.getPaymentVia());

        paymentEntity = paymentRepository.save(paymentEntity);

        bill.setStatus(PaymentStatus.PAID);
        billRepository.save(bill);

        subscription.setIsActive(true);
        subscriptionRepository.save(subscription);

        return paymentMapper.toPaymentResponse(paymentEntity);
    }

    public PaymentResponse getPaymentByBillId(UUID billId) {
        PaymentEntity payment = paymentRepository.findByBillBillId(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        return paymentMapper.toPaymentResponse(payment);
    }
}

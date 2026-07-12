package com.example.tds.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.tds.mapper.BillMapper;
import com.example.tds.entity.BillEntity;
import com.example.tds.enums.PaymentStatus;
import org.springframework.stereotype.Service;
import com.example.tds.repository.BillRepository;
import com.example.tds.entity.SubscriptionEntity;
import com.example.tds.dto.responses.BillResponse;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final BillMapper billMapper;
    private final RazorpayService razorpayService;

    @Transactional
    public BillResponse createBill(SubscriptionEntity subscription, String name, double amount) {
        try {
            BillEntity billEntity = new BillEntity();

            String orderId = razorpayService.createOrder(
                    amount,
                    "INR",
                    name,
                    subscription.getSubscriptionId()
            );

            billEntity.setSubscription(subscription);
            billEntity.setName(name);
            billEntity.setAmount(amount);
            billEntity.setStatus(PaymentStatus.PENDING);

            billEntity.setOrderId(orderId);

            billEntity = billRepository.save(billEntity);

            return billMapper.toBillResponse(billEntity);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create bill", e);
        }
    }
}
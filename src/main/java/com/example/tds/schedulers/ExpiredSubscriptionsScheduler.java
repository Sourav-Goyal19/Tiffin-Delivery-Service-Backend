package com.example.tds.schedulers;

import com.example.tds.repository.SubscriptionRepository;
import com.example.tds.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredSubscriptionsScheduler {
    private final SubscriptionService subscriptionService;
    @Scheduled(
            cron = "0 0 0 * * *",
            zone = "Asia/Kolkata"
    )
    public void deactivateExpiredSubscriptions() {
        subscriptionService.deactivateExpiredSubscriptions();
    }
}

package com.example.tds.service;

import com.example.tds.entity.ChefEntity;
import com.example.tds.entity.UserEntity;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.repository.ChefRepository;
import com.example.tds.repository.SubscriptionRepository;
import com.example.tds.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {
    private final ChefRepository chefRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${distance.rate.per.km}")
    private int distanceRatePerKm;

    @Value("${distance.base.fee}")
    private int distanceBaseFee;

    public double calculateDeliveryFee(UUID userId, UUID chefId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ChefEntity chef = chefRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef not found"));

        double distanceInKm = subscriptionRepository.findDisInKm(user.getLocation(), chef.getLocation()).getDisInKm();
        int distance = (int) Math.ceil(distanceInKm);

        double fees = (distanceBaseFee + (distance * distanceRatePerKm));
        int fee = (int) fees / 10;
        return fee * 10.0;
    }
}

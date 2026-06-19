package com.example.tds.service;

import com.example.tds.dto.responses.*;
import com.example.tds.entity.*;
import com.example.tds.enums.DeliveryType;
import com.example.tds.enums.PlanType;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.mapper.SubscriptionMapper;
import com.example.tds.repository.UserRepository;
import com.example.tds.repository.MealPlanRepository;
import com.example.tds.exception.BadRequestException;
import com.example.tds.repository.SubscriptionRepository;
import com.example.tds.projection.SubscriptionWithDetailsProjection;
import com.example.tds.dto.requests.subscriptions.CreateSubscriptionRequest;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final UserRepository userRepository;
    private final MealPlanRepository mealPlanRepository;
    private final BillService billService;

    @Value("${distance.rate.per.km}")
    private int distanceRatePerKm;

    @Value("${distance.base.fee}")
    private int distanceBaseFee;

    public Map<String,Object> createSubscription(UUID userId, CreateSubscriptionRequest createSubscriptionRequest) {
        UUID mealPlanId = createSubscriptionRequest.getMealPlanId();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(()-> new BadRequestException("Invalid user id"));

        MealPlanEntity mealPlan = mealPlanRepository.findByMealPlanId(mealPlanId)
                .orElseThrow(()-> new BadRequestException("Invalid meal plan id"));

        ChefEntity chef = mealPlan.getChef();

        double distanceInKm = subscriptionRepository.findDisInKm(user.getLocation(), chef.getLocation()).getDisInKm();
        int distance = (int) Math.ceil(distanceInKm);

        DeliveryType deliveryType = createSubscriptionRequest.getDeliveryType();

        double deliveryFee = deliveryType == DeliveryType.DELIVERY ? calculateDeliveryFee(distance) : 0.0;

        PlanType planType = createSubscriptionRequest.getPlanType();

        SubscriptionEntity subscription = subscriptionMapper.toSubscriptionEntity(createSubscriptionRequest);

        subscription.setUser(user);
        subscription.setMealPlan(mealPlan);

        LocalDate startDate = LocalDate.now().plusDays(1);
        subscription.setStartDate(startDate);

        double totalDeliveryFee = deliveryFee;

        if(planType == PlanType.WEEKLY){
            subscription.setPrice(mealPlan.getWeeklyPrice());
            subscription.setEndDate(startDate.plusDays(7));
            totalDeliveryFee = deliveryFee * 7;
            subscription.setDeliveryFee(totalDeliveryFee);
        }
        else if(planType == PlanType.MONTHLY){
            subscription.setPrice(mealPlan.getMonthlyPrice());
            subscription.setEndDate(startDate.plusDays(30));
            totalDeliveryFee = deliveryFee * 30;
            subscription.setDeliveryFee(totalDeliveryFee);
        }

        subscription = subscriptionRepository.save(subscription);

        double totalAmount = subscription.getPrice() + totalDeliveryFee;

        BillResponse bill = billService.createBill(subscription, user.getName(), totalAmount);
        SubscriptionResponse subscriptionResponse = subscriptionMapper.toSubscriptionResponse(subscription);

        return Map.of("bill", bill, "subscription", subscriptionResponse);
    }

    public SubscriptionWithDetailsResponse getSubscription(UUID subscriptionId) {
        SubscriptionWithDetailsProjection projection = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(()-> new ResourceNotFoundException("Subscription not found"));

        return mapToSubscriptionWithDetailsResponse(projection);
    }

    public List<SubscriptionWithDetailsResponse> getSubscriptionsByUserId(UUID userId) {
        List<SubscriptionWithDetailsProjection> projections = subscriptionRepository.findBySubscriptionUserId(userId);
        return projections.stream().map(this::mapToSubscriptionWithDetailsResponse).toList();
    }

    private double calculateDeliveryFee(int distance) {
        double fees = (distanceBaseFee + (distance * distanceRatePerKm));
        int fee = (int) fees / 10;
        return fee * 10;
    }

    private SubscriptionWithDetailsResponse mapToSubscriptionWithDetailsResponse(SubscriptionWithDetailsProjection projection) {
        SubscriptionWithDetailsResponse response = new SubscriptionWithDetailsResponse();

        SubscriptionResponse subscription = SubscriptionResponse.builder()
                .subscriptionId(projection.getSubscriptionId())
                .userId(projection.getUserId())
                .mealPlanId(projection.getMealPlanId())
                .deliveryType(projection.getDeliveryType())
                .planType(projection.getPlanType())
                .isActive(projection.getIsActive())
                .price(projection.getPrice())
                .deliveryFee(projection.getDeliveryFee())
                .startDate(projection.getStartDate())
                .endDate(projection.getEndDate())
                .createdAt(projection.getSubscriptionCreatedAt())
                .updatedAt(projection.getSubscriptionUpdatedAt())
                .build();

        UserResponse user = UserResponse.builder()
                .id(projection.getUserId())
                .name(projection.getUserName())
                .address(projection.getUserAddress())
                .mobileNo(projection.getUserMobileNo())
                .createdAt(projection.getUserCreatedAt())
                .updatedAt(projection.getUserUpdatedAt())
                .build();

        ChefResponse chef = ChefResponse.builder()
                .chefId(projection.getChefId())
                .name(projection.getChefName())
                .mobileNo(projection.getChefMobileNo())
                .address(projection.getChefAddress())
                .rating(projection.getChefRating())
                .createdAt(projection.getChefCreatedAt())
                .updatedAt(projection.getChefUpdatedAt())
                .build();

        MealPlanResponse mealPlan = MealPlanResponse.builder()
                .mealPlanId(projection.getMealPlanId())
                .chefId(projection.getChefId())
                .mealType(projection.getMealType())
                .weeklyPrice(projection.getWeeklyPrice())
                .monthlyPrice(projection.getMonthlyPrice())
                .timing(projection.getTiming())
                .capacity(projection.getCapacity())
                .createdAt(projection.getMealPlanCreatedAt())
                .updatedAt(projection.getMealPlanUpdatedAt())
                .build();

        response.setSubscription(subscription);
        response.setUser(user);
        response.setChef(chef);
        response.setMealPlan(mealPlan);

        return response;
    }
}


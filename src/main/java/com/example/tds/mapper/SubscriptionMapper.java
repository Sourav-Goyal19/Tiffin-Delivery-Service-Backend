package com.example.tds.mapper;

import com.example.tds.dto.requests.subscriptions.CreateSubscriptionRequest;
import com.example.tds.dto.responses.SubscriptionResponse;
import com.example.tds.entity.SubscriptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "mealPlan.mealPlanId", target = "mealPlanId")
    SubscriptionResponse toSubscriptionResponse(SubscriptionEntity subscriptionEntity);
    SubscriptionEntity toSubscriptionEntity(CreateSubscriptionRequest subscriptionRequest);
}

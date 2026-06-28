package com.example.tds.mapper;

import com.example.tds.dto.responses.OrderResponse;
import com.example.tds.entity.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(source = "subscription.subscriptionId", target = "subscriptionId")
    @Mapping(source = "deliveryAgent.deliveryAgentId", target = "deliveryAgentId")
    OrderResponse toOrderResponse(OrderEntity orderEntity);
}

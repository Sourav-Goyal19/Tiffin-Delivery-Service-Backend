package com.example.tds.mapper;

import com.example.tds.dto.responses.OrderResponse;
import com.example.tds.dto.responses.CustomerOrderResponse;
import com.example.tds.dto.responses.ChefOrderResponse;
import com.example.tds.dto.responses.DeliveryAgentOrderResponse;
import com.example.tds.entity.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface OrderMapper {
    @Mapping(source = "subscription.subscriptionId", target = "subscriptionId")
    @Mapping(source = "deliveryAgent.deliveryAgentId", target = "deliveryAgentId")
    OrderResponse toOrderResponse(OrderEntity orderEntity);

    @Mapping(source = "subscription.subscriptionId", target = "subscriptionId")
    @Mapping(source = "deliveryAgent.deliveryAgentId", target = "deliveryAgentId")
    CustomerOrderResponse toCustomerOrderResponse(OrderEntity orderEntity);

    @Mapping(source = "subscription.subscriptionId", target = "subscriptionId")
    @Mapping(source = "deliveryAgent.deliveryAgentId", target = "deliveryAgentId")
    ChefOrderResponse toChefOrderResponse(OrderEntity orderEntity);
    
    @Mapping(source = "subscription.subscriptionId", target = "subscriptionId")
    @Mapping(source = "deliveryAgent.deliveryAgentId", target = "deliveryAgentId")
    @Mapping(source = "subscription.deliveryAgentFee", target = "deliveryAgentFee")
    DeliveryAgentOrderResponse toDeliveryAgentOrderResponse(OrderEntity orderEntity);
}

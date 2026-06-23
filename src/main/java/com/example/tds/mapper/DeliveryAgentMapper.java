package com.example.tds.mapper;

import com.example.tds.dto.responses.DeliveryAgentResponse;
import com.example.tds.entity.DeliveryAgentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeliveryAgentMapper {
    DeliveryAgentResponse toDeliveryAgentResponse(DeliveryAgentEntity entity);
}

package com.example.tds.mapper;

import com.example.tds.dto.responses.BillResponse;
import com.example.tds.entity.BillEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BillMapper {
    @Mapping(source = "subscription.subscriptionId", target = "subscriptionId")
    BillResponse toBillResponse(BillEntity billEntity);
}

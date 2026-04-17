package com.example.tds.mapper;

import com.example.tds.dto.responses.ChefResponse;
import com.example.tds.dto.requests.ChefSignUpRequest;
import com.example.tds.entity.ChefEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChefMapper {
    ChefEntity toChefEntity(ChefSignUpRequest createRequest);

    ChefResponse toChefResponse(ChefEntity chefEntity);
}

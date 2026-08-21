package com.example.tds.mapper;

import com.example.tds.dto.responses.*;
import com.example.tds.entity.UserEntity;
import com.example.tds.dto.requests.users.UserSignUpRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    UserEntity toUserEntity(UserSignUpRequest dto);

    UserResponse toUserResponse(UserEntity user);
}

package com.example.tds.mapper;

import com.example.tds.dto.responses.MenuWithDistanceResponse;
import com.example.tds.projection.MenuWithDistanceProjection;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMenuMapper {
    List<MenuWithDistanceResponse> toMenuWithDistanceResponse(List<MenuWithDistanceProjection> menusWithDistance);

    MenuWithDistanceResponse toMenuWithDistanceResponse(MenuWithDistanceProjection projection);
}

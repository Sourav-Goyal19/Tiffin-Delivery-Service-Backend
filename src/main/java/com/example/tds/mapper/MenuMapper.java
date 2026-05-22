package com.example.tds.mapper;

import com.example.tds.dto.requests.menu.CreateMenuRequest;
import com.example.tds.dto.responses.MenuResponse;
import com.example.tds.entity.MenuEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MenuMapper {
    MenuEntity toMenuEntity(CreateMenuRequest menuRequest);
    List<MenuEntity> toMenuEntities(List<CreateMenuRequest> menuRequests);

    MenuResponse toMenuResponse(MenuEntity menuEntity);
    List<MenuResponse> toMenuResponse(List<MenuEntity> menuEntities);
}

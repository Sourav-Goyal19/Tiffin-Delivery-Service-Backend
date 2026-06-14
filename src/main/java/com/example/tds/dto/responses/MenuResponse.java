package com.example.tds.dto.responses;

import com.example.tds.enums.MealType;
import com.example.tds.enums.WeekDay;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuResponse {
    private UUID menuId;
    private WeekDay weekDay;
    private MealType mealType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> items;
    private Boolean isActive;
    private String thumbnailUrl;
    private UUID chefId;
}

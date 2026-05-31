package com.example.tds.dto.requests.menu;

import com.example.tds.enums.MealType;
import com.example.tds.enums.WeekDay;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateMenuRequest {
    @NotNull(message = "items field is required")
    private List<String> items;

    @NotNull(message = "A proper weekDay field is required")
    private WeekDay weekDay;

    @NotNull(message = "A proper mealType field is required")
    private MealType mealType;

    @NotNull(message = "A isActive field is required")
    private Boolean isActive;

    private String thumbnailUrl;
}

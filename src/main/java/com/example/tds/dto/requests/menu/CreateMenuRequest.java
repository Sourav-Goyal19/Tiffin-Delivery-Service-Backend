package com.example.tds.dto.requests.menu;

import com.example.tds.enums.MealType;
import com.example.tds.enums.WeekDay;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CreateMenuRequest {
    @NotNull(message = "items data is required")
    private List<String> items;

    @NotNull(message = "A proper weekDay is required")
    private WeekDay weekDay;

    @NotNull(message = "A proper meal type is required")
    private MealType mealType;
}

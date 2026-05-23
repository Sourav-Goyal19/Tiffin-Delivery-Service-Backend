package com.example.tds.dto.requests.menu;

import com.example.tds.enums.WeekDay;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CreateMenuRequest {
    @NotNull(message = "items data is required")
    private Map<String, Object> items;

    @NotNull(message = "A proper weekDay is required")
    private WeekDay weekDay;
}

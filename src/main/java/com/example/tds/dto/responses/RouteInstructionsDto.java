package com.example.tds.dto.responses;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteInstructionsDto {
    private String instruction;
    private double distance;
    private long timeInSeconds;
    private String streetName;
}
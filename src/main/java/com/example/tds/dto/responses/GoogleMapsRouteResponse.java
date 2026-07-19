package com.example.tds.dto.responses;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleMapsRouteResponse {
    private double distanceInMeters;
    private long durationInSeconds;
    private String encodedPolyline;
    private List<RouteInstructionsDto> instructions;
}

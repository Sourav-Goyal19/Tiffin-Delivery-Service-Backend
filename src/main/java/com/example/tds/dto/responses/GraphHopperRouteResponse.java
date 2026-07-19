package com.example.tds.dto.responses;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphHopperRouteResponse {
    private double distanceInMeters;
    private long durationInSeconds;
    private String encodedPolyline;
    private List<RouteInstructionsDto> instructions;
}
package com.example.tds.dto.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleMapsApiResponse {
    private List<Route> routes;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        private int distanceMeters;
        private String duration;
        private Polyline polyline;
        private List<Leg> legs;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Leg {
        private int distanceMeters;
        private String duration;
        private List<Step> steps;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Step {
        private int distanceMeters;
        private String duration;
        @JsonProperty("staticDuration")
        private String staticDuration;
        private Polyline polyline;
        @JsonProperty("navigationInstruction")
        private NavigationInstruction navigationInstruction;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NavigationInstruction {
        private String instructions;
        private String maneuver;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Polyline {
        private String encodedPolyline;
    }
}

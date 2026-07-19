package com.example.tds.dto.responses;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphHopperApiResponse {
    private List<GraphHopperPath> paths;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphHopperPath {
        private double distance;
        private long time;
        private String points;
        private List<GraphHopperInstruction> instructions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphHopperInstruction {
        private String text;
        private double distance;
        private long time;
        
        @JsonProperty("street_name")
        private String streetName;
    }
}

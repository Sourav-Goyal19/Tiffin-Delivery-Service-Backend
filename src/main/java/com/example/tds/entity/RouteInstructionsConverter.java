package com.example.tds.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.example.tds.dto.responses.RouteInstructionsDto;

import java.util.List;

@Converter
public class RouteInstructionsConverter implements AttributeConverter<List<RouteInstructionsDto>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<RouteInstructionsDto> instructions) {
        if (instructions == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(instructions);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting list of RouteInstructionsDto to JSON string", e);
        }
    }

    @Override
    public List<RouteInstructionsDto> convertToEntityAttribute(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<RouteInstructionsDto>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON string to list of RouteInstructionsDto", e);
        }
    }
}

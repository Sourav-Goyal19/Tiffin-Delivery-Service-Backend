package com.example.tds.dto.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChefResponse {
    private UUID chefId;
    private String name;
    private String address;
    private String mobileNo;
    private String avatarUrl;
    private Double rating;
    private Double longitude;
    private Double latitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Double disInKm;
    private String refreshToken;
    private String accessToken;
}

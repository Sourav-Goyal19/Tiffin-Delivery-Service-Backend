package com.example.tds.dto.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChefResponse {
    private UUID chefId;
    private String name;
    private String address;
    private String mobileNo;
    private Double dayPrice;
    private Double weeklyPrice;
    private Double monthlyPrice;

    private String refreshToken;
    private String accessToken;
}

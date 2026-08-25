package com.example.tds.dto.requests.common;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    @Size(max = 2048, message = "refreshToken must not exceed 2048 characters")
    private String refreshToken;
}

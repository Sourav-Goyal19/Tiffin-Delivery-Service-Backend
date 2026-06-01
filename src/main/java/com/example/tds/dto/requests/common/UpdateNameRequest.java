package com.example.tds.dto.requests.common;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateNameRequest {
    @NotBlank(message = "name is required")
    private String name;
}

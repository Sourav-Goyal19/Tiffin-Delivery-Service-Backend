package com.example.tds.dto.requests.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserNameUpdateRequest {
    @NotBlank(message = "name is required")
    private String name;
}

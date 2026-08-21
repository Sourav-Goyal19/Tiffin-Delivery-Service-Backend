package com.example.tds.controller;

import com.example.tds.dto.responses.ApiResponse;
import com.example.tds.entity.UserEntity;
import com.example.tds.service.UserChefSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/chefs")
@RequiredArgsConstructor
@Validated
public class UserChefSearchController {

    private final UserChefSearchService userChefSearchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchChefs(
            @PathVariable("userId") UUID userId,
            @RequestAttribute("user") UserEntity currentUser,
            @RequestParam("q")
            @NotBlank(message = "Search query is required")
            @Size(max = 100, message = "Search query must not exceed 100 characters")
            String q,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "page must be at least 1")
            Integer page,
            @RequestParam(value = "limit", required = false, defaultValue = "20")
            @Min(value = 1, message = "limit must be at least 1")
            @Max(value = 50, message = "limit must not exceed 50")
            Integer limit
    ) {
        Map<String, Object> data = userChefSearchService.handleSearchChefs(
                userId, currentUser, q, page, limit
        );

        boolean hasResults = !((List<?>) data.get("chefs")).isEmpty();
        String message = hasResults ? "Chefs found successfully" : "No chefs found";

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message(message)
                        .success(true)
                        .data(data)
                        .build()
        );
    }
}
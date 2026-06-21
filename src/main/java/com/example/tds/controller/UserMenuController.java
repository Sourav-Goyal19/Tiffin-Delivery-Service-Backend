package com.example.tds.controller;

import com.example.tds.dto.responses.ApiResponse;
import com.example.tds.dto.responses.MenuWithDistanceResponse;
import com.example.tds.entity.UserEntity;
import com.example.tds.service.UserMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/user-menus")
@RequiredArgsConstructor
public class UserMenuController {
    private final UserMenuService userMenuService;

    @GetMapping()
    public ResponseEntity<ApiResponse> getMenuForUsers(@PathVariable("userId") UUID userId) {
        List<MenuWithDistanceResponse> menus = userMenuService.handleGetMenusForCustomers(userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Menus found successfully")
                        .success(true)
                        .data(Map.of("menus", menus))
                        .build()
        );
    }

    @GetMapping("/chefs/{chefId}")
    public ResponseEntity<ApiResponse> getChefMenuForUsers(@PathVariable("userId") UUID userId, @PathVariable("chefId") UUID chefId) {
        List<MenuWithDistanceResponse> menus = userMenuService.handleGetChefMenuForUsers(userId, chefId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("Menus found successfully")
                        .success(true)
                        .data(Map.of("menus", menus))
                        .build()
        );
    }
}

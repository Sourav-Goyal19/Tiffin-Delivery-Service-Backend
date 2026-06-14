package com.example.tds.service;

import com.example.tds.dto.responses.ChefResponse;
import com.example.tds.dto.responses.MealPlanResponse;
import com.example.tds.dto.responses.MenuResponse;
import com.example.tds.dto.responses.MenuWithDistanceResponse;
import com.example.tds.projection.MenuWithDistanceProjection;
import com.example.tds.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMenuService {

    private final MenuRepository menuRepository;

    public List<MenuWithDistanceResponse> handleGetMenusForCustomers(UUID userId) {

        List<MenuWithDistanceProjection> menusWithDistance =
                menuRepository.findAllByDistance(userId)
                        .orElse(List.of());

        return menusWithDistance.stream()
                .map(md -> {
                    MenuWithDistanceResponse response = new MenuWithDistanceResponse();

                    response.setMenu(
                            MenuResponse.builder()
                                    .menuId(md.getMenuId())
                                    .items(md.getItems())
                                    .mealType(md.getMealType())
                                    .isActive(md.getIsActive())
                                    .chefId(md.getChefId())
                                    .weekDay(md.getWeekDay())
                                    .thumbnailUrl(md.getThumbnailUrl())
                                    .createdAt(md.getCreatedAt())
                                    .updatedAt(md.getUpdatedAt())
                                    .build()
                    );

                    response.setChef(
                            ChefResponse.builder()
                                    .chefId(md.getChefId())
                                    .name(md.getName())
                                    .address(md.getAddress())
                                    .mobileNo(md.getMobileNo())
                                    .avatarUrl(md.getAvatarUrl())
                                    .rating(md.getRating())
                                    .disInKm(md.getDisInKm())
                                    .createdAt(md.getChefCreatedAt())
                                    .updatedAt(md.getChefUpdatedAt())
                                    .build()
                    );

                    response.setMealPlan(
                            MealPlanResponse.builder()
                                    .mealPlanId(md.getMealPlanId())
                                    .mealType(md.getMealType())
                                    .weeklyPrice(md.getWeeklyPrice())
                                    .monthlyPrice(md.getMonthlyPrice())
                                    .capacity(md.getMealPlanCapacity())
                                    .timing(md.getTiming())
                                    .chefId(md.getChefId())
                                    .isActive(md.getMealPlanIsActive())
                                    .createdAt(md.getMealPlanCreatedAt())
                                    .updatedAt(md.getMealPlanUpdatedAt())
                                    .build()
                    );

                    return response;
                })
                .toList();
    }

    public List<MenuWithDistanceResponse> handleGetChefMenuForUsers(UUID userId, UUID chefId) {
        List<MenuWithDistanceProjection> menusWithDistance =
                menuRepository.findMenuByChefId(userId, chefId)
                        .orElse(List.of());

        return menusWithDistance.stream()
                .map(md -> {
                    MenuWithDistanceResponse response = new MenuWithDistanceResponse();

                    response.setMenu(
                            MenuResponse.builder()
                                    .menuId(md.getMenuId())
                                    .items(md.getItems())
                                    .mealType(md.getMealType())
                                    .isActive(md.getIsActive())
                                    .chefId(md.getChefId())
                                    .weekDay(md.getWeekDay())
                                    .thumbnailUrl(md.getThumbnailUrl())
                                    .createdAt(md.getCreatedAt())
                                    .updatedAt(md.getUpdatedAt())
                                    .build()
                    );

                    response.setChef(
                            ChefResponse.builder()
                                    .chefId(md.getChefId())
                                    .name(md.getName())
                                    .address(md.getAddress())
                                    .mobileNo(md.getMobileNo())
                                    .avatarUrl(md.getAvatarUrl())
                                    .rating(md.getRating())
                                    .disInKm(md.getDisInKm())
                                    .createdAt(md.getChefCreatedAt())
                                    .updatedAt(md.getChefUpdatedAt())
                                    .build()
                    );

                    response.setMealPlan(
                            MealPlanResponse.builder()
                                    .mealPlanId(md.getMealPlanId())
                                    .mealType(md.getMealType())
                                    .weeklyPrice(md.getWeeklyPrice())
                                    .monthlyPrice(md.getMonthlyPrice())
                                    .capacity(md.getMealPlanCapacity())
                                    .timing(md.getTiming())
                                    .chefId(md.getChefId())
                                    .isActive(md.getMealPlanIsActive())
                                    .createdAt(md.getMealPlanCreatedAt())
                                    .updatedAt(md.getMealPlanUpdatedAt())
                                    .build()
                    );

                    return response;
                })
                .toList();
    }
}

package com.example.tds.repository;

import com.example.tds.entity.MenuEntity;
import com.example.tds.enums.MealType;
import com.example.tds.enums.WeekDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuRepository extends JpaRepository<MenuEntity, UUID> {
    Optional<MenuEntity> findByMenuId(UUID menuId);
    Optional<List<MenuEntity>> findAllByChefChefId(UUID chefId);
    Optional<MenuEntity> findByChefChefIdAndMealTypeAndWeekDayAndIsActiveTrue(UUID chefId, MealType mealType, WeekDay weekDay);
}

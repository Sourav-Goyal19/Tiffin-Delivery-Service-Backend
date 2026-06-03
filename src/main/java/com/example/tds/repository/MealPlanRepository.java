package com.example.tds.repository;

import com.example.tds.entity.MealPlanEntity;
import com.example.tds.enums.MealType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface MealPlanRepository extends JpaRepository<MealPlanEntity, UUID> {
    Optional<MealPlanEntity> findByMealPlanId(UUID mealPlanId);

    Optional<MealPlanEntity> findByChefChefIdAndMealPlanId(UUID chefId, UUID mealPlanId);

    Optional<List<MealPlanEntity>> findAllByChefChefId(UUID chefId);

    Optional<MealPlanEntity> findByChefChefIdAndMealType(UUID chefChefId, MealType mealType);
}

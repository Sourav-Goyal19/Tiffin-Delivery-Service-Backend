package com.example.tds.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.tds.entity.ChefEntity;
import com.example.tds.entity.MealPlanEntity;
import com.example.tds.mapper.MealPlanMapper;
import org.springframework.stereotype.Service;
import com.example.tds.repository.MealPlanRepository;
import com.example.tds.exception.BadRequestException;
import com.example.tds.dto.responses.MealPlanResponse;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.dto.requests.mealplans.CreateMealPlanRequest;
import com.example.tds.dto.requests.mealplans.UpdateMealPlanRequest;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanService {
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanMapper mealPlanMapper;

    public MealPlanResponse handleCreateMealPlan(ChefEntity chef, CreateMealPlanRequest mealPlanRequest) {
        MealPlanEntity mealPlan = mealPlanMapper.toMealPlanEntity(mealPlanRequest);

        MealPlanEntity existingMealPlan = mealPlanRepository.findByChefChefIdAndMealType(
                chef.getChefId(),
                mealPlan.getMealType()
        ).orElse(null);

        if (existingMealPlan != null) {
            throw new BadRequestException("Meal Plan already exists");
        }

        mealPlan.setChef(chef);

        mealPlan = mealPlanRepository.save(mealPlan);

        return mealPlanMapper.toMealPlanResponse(mealPlan);
    }

    public MealPlanResponse handleGetMealPlan(UUID chefId, UUID mealPlanId) {
        MealPlanEntity mealPlan = findMealPlanByChefIdAndMealPlanId(chefId, mealPlanId);

        return mealPlanMapper.toMealPlanResponse(mealPlan);
    }

    public List<MealPlanResponse> handleGetAllMealPlans(UUID chefId) {
        List<MealPlanEntity> mealPlans = mealPlanRepository.findAllByChefChefId(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("No meal plans found"));

        if (mealPlans.isEmpty()) {
            throw new ResourceNotFoundException("No meal plans found");
        }

        return mealPlanMapper.toMealPlanResponse(mealPlans);
    }

    public MealPlanResponse handleUpdateMealPlan(UUID chefId, UUID mealPlanId, UpdateMealPlanRequest mealPlanRequest) {
        MealPlanEntity existingMealPlan = findMealPlanByChefIdAndMealPlanId(chefId, mealPlanId);
        MealPlanEntity updatedMealPlan = mealPlanMapper.toMealPlanEntity(mealPlanRequest);

        existingMealPlan.setWeeklyPrice(updatedMealPlan.getWeeklyPrice());
        existingMealPlan.setMonthlyPrice(updatedMealPlan.getMonthlyPrice());
        existingMealPlan.setTiming(updatedMealPlan.getTiming());
        existingMealPlan.setIsActive(updatedMealPlan.getIsActive());

        updatedMealPlan = mealPlanRepository.save(existingMealPlan);

        return mealPlanMapper.toMealPlanResponse(updatedMealPlan);
    }

    public void handleDeleteMealPlan(UUID chefId, UUID mealPlanId) {
        MealPlanEntity existingMealPlan = findMealPlanByChefIdAndMealPlanId(chefId, mealPlanId);

        // TODO: delete only when there is/are no active subscription(s)

        mealPlanRepository.delete(existingMealPlan);
    }

    private MealPlanEntity findMealPlanByChefIdAndMealPlanId(UUID chefId, UUID mealPlanId) {
        return mealPlanRepository.findByChefChefIdAndMealPlanId(chefId, mealPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal Plan not found"));
    }
}

package com.example.tds.repository;

import com.example.tds.entity.MenuEntity;
import com.example.tds.enums.MealType;
import com.example.tds.enums.WeekDay;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.tds.projection.MenuWithDistanceProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuRepository extends JpaRepository<MenuEntity, UUID> {
    Optional<MenuEntity> findByMenuId(UUID menuId);
    Optional<List<MenuEntity>> findAllByChefChefId(UUID chefId);
    Optional<MenuEntity> findByChefChefIdAndMealTypeAndWeekDayAndIsActiveTrue(UUID chefId, MealType mealType, WeekDay weekDay);

    @Query(value = """
        SELECT
            m.menu_id         AS "menuId",
            m.meal_type       AS "mealType",
            m.items           AS "items",
            m.thumbnail_url   AS "thumbnailUrl",
            m.is_active       AS "isActive",
            m.week_day        AS "weekDay",
            m.created_at      AS "createdAt",
            m.updated_at      AS "updatedAt",

            c.chef_id         AS "chefId",
            c.name            AS "name",
            c.address         AS "address",
            c.mobile_no       AS "mobileNo",
            c.avatar_url      AS "avatarUrl",
            c.rating          AS "rating",
            c.created_at      AS "chefCreatedAt",
            c.updated_at      AS "chefUpdatedAt",
            ROUND((ST_Distance(c.location::geography, u.location::geography) / 1000.0)::numeric, 2) AS "disInKm",

            mp.meal_plan_id  AS "mealPlanId",
            mp.weekly_price  AS "weeklyPrice",
            mp.monthly_price AS "monthlyPrice",
            mp.capacity      AS "mealPlanCapacity",
            mp.timing        AS "timing",
            mp.is_active     AS "mealPlanIsActive",
            mp.created_at    AS "mealPlanCreatedAt",
            mp.updated_at    AS "mealPlanUpdatedAt"
        FROM menus m
        JOIN chefs c
            ON c.chef_id = m.chef_id
        JOIN meal_plans mp
            ON m.meal_type = mp.meal_type
            AND m.chef_id = mp.chef_id
            AND mp.is_active = true
        JOIN users u
            ON u.user_id = :userId
        WHERE ST_DWithin(
            c.location,
            u.location,
            5000
        ) AND m.is_active = true
        ORDER BY "disInKm",
            CASE
                    WHEN m.meal_type = 'BREAKFAST' THEN 1
                    WHEN m.meal_type = 'LUNCH' THEN 2
                    WHEN m.meal_type = 'DINNER' THEN 3
                    ELSE 4
            END;
    """, nativeQuery = true)
    Optional<List<MenuWithDistanceProjection>> findAllByDistance(UUID userId);

    @Query(nativeQuery = true, value = """
        SELECT
            m.menu_id         AS "menuId",
            m.meal_type       AS "mealType",
            m.items           AS "items",
            m.thumbnail_url   AS "thumbnailUrl",
            m.is_active       AS "isActive",
            m.week_day        AS "weekDay",
            m.created_at      AS "createdAt",
            m.updated_at      AS "updatedAt",

            c.chef_id         AS "chefId",
            c.name            AS "name",
            c.address         AS "address",
            c.mobile_no       AS "mobileNo",
            c.avatar_url      AS "avatarUrl",
            c.rating          AS "rating",
            c.created_at      AS "chefCreatedAt",
            c.updated_at      AS "chefUpdatedAt",
            ROUND((ST_Distance(c.location::geography, u.location::geography) / 1000.0)::numeric, 2) AS "disInKm",

            mp.meal_plan_id  AS "mealPlanId",
            mp.weekly_price  AS "weeklyPrice",
            mp.monthly_price AS "monthlyPrice",
            mp.capacity      AS "mealPlanCapacity",
            mp.timing        AS "timing",
            mp.is_active     AS "mealPlanIsActive",
            mp.created_at    AS "mealPlanCreatedAt",
            mp.updated_at    AS "mealPlanUpdatedAt"
        FROM menus m
        JOIN chefs c
            ON c.chef_id = m.chef_id
        JOIN meal_plans mp
            ON mp.meal_type = m.meal_type
            AND mp.chef_id = c.chef_id
            AND mp.is_active = true
        JOIN users u
            ON u.user_id = :userId
        WHERE m.is_active = true AND c.chef_id = :chefId
        ORDER BY
            CASE
                WHEN m.meal_type = 'BREAKFAST' THEN 1
                WHEN m.meal_type = 'LUNCH' THEN 2
                WHEN m.meal_type = 'DINNER' THEN 3
                ELSE 4
            END;
     """)
    Optional<List<MenuWithDistanceProjection>> findMenuByChefId(UUID userId, UUID chefId);
}

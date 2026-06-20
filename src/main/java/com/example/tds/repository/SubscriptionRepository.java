package com.example.tds.repository;

import com.example.tds.entity.SubscriptionEntity;
import com.example.tds.projection.DistanceProjection;
import com.example.tds.projection.SubscriptionWithDetailsProjection;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {
    @Query(nativeQuery = true, value = """
        SELECT 
            s.subscription_id  AS "subscriptionId",
            s.user_id          AS "userId",
            s.meal_plan_id     AS "mealPlanId",
            s.delivery_type    AS "deliveryType",
            s.plan_type        AS "planType",
            s.is_active        AS "isActive",
            s.price            AS "price",
            s.start_date       AS "startDate",
            s.end_date         AS "endDate",
            s.created_at       AS "subscriptionCreatedAt",
            s.updated_at       AS "subscriptionUpdatedAt",
            
            u.name             AS "userName",
            u.mobile_no        AS "userMobileNo",
            u.address          AS "userAddress",
            u.created_at       AS "userCreatedAt",
            u.updated_at       AS "userUpdatedAt",
            
            mp.chef_id         AS "chefId",
            mp.meal_type       AS "mealType",
            mp.weekly_price    AS "weeklyPrice",
            mp.monthly_price   AS "monthlyPrice",
            mp.timing          AS "timing",
            mp.capacity        AS "capacity",
            mp.created_at      AS "mealPlanCreatedAt",
            mp.updated_at      AS "mealPlanUpdatedAt",
                
            c.name             AS "chefName",
            c.mobile_no        AS "chefMobileNo",
            c.address          AS "chefAddress",
            c.rating           AS "chefRating",
            c.created_at       AS "chefCreatedAt",
            c.updated_at       AS "chefUpdatedAt"
        FROM subscriptions s
        JOIN users u 
            ON u.user_id = s.user_id
        JOIN meal_plans mp
            ON mp.meal_plan_id = s.meal_plan_id
        JOIN chefs c
            ON c.chef_id = mp.chef_id
        WHERE u.user_id = :userId AND s.is_active = true
        ORDER BY "subscriptionCreatedAt"; 
    """)
    List<SubscriptionWithDetailsProjection> findBySubscriptionUserId(UUID userId);

    @Query(nativeQuery = true, value = """
        SELECT 
            s.subscription_id  AS "subscriptionId",
            s.user_id          AS "userId",
            s.meal_plan_id     AS "mealPlanId",
            s.delivery_type    AS "deliveryType",
            s.plan_type        AS "planType",
            s.is_active        AS "isActive",
            s.price            AS "price",
            s.delivery_fee     AS "deliveryFee",
            s.start_date       AS "startDate",
            s.end_date         AS "endDate",
            s.created_at       AS "subscriptionCreatedAt",
            s.updated_at       AS "subscriptionUpdatedAt",
            
            u.name             AS "userName",
            u.mobile_no        AS "userMobileNo",
            u.address          AS "userAddress",
            u.created_at       AS "userCreatedAt",
            u.updated_at       AS "userUpdatedAt",
            
            mp.chef_id         AS "chefId",
            mp.meal_type       AS "mealType",
            mp.weekly_price    AS "weeklyPrice",
            mp.monthly_price   AS "monthlyPrice",
            mp.timing          AS "timing",
            mp.capacity        AS "capacity",
            mp.created_at      AS "mealPlanCreatedAt",
            mp.updated_at      AS "mealPlanUpdatedAt",
                
            c.name             AS "chefName",
            c.mobile_no        AS "chefMobileNo",
            c.address          AS "chefAddress",
            c.rating           AS "chefRating",
            c.created_at       AS "chefCreatedAt",
            c.updated_at       AS "chefUpdatedAt"
        FROM subscriptions s
        JOIN users u 
            ON u.user_id = s.user_id
        JOIN meal_plans mp
            ON mp.meal_plan_id = s.meal_plan_id
        JOIN chefs c
            ON c.chef_id = mp.chef_id
        WHERE s.subscription_id = :subscriptionId
        ORDER BY "subscriptionCreatedAt"; 
    """)
    Optional<SubscriptionWithDetailsProjection> findBySubscriptionId(UUID subscriptionId);

    @Query(value = """
    SELECT ROUND(
        (ST_Distance(
            CAST(:userLocation AS geography),
            CAST(:chefLocation AS geography)
        ) / 1000.0)::numeric,
        2
    ) AS disInKm
    """, nativeQuery = true)
    DistanceProjection findDisInKm(@Param("userLocation") Point userLocation, @Param("chefLocation") Point chefLocation);

    Optional<SubscriptionEntity> findByUserIdAndMealPlanMealPlanIdAndIsActive(UUID userId, UUID mealPlanId, boolean isActive);
}

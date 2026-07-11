package com.example.tds.repository;

import com.example.tds.entity.OrderEntity;
import org.springframework.data.jpa.repository.Query;
import com.example.tds.projection.OrderForChefProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByOrderId(UUID orderId);

    @Query(nativeQuery = true, value = """
        SELECT 
            o.order_id            AS "orderId",
            o.delivery_agent_id   AS "deliveryAgentId",
            o.from_location       AS "fromLocation",
            o.to_location         AS "toLocation",
            o.status              AS "status",
            o.order_date          AS "orderDate",
            o.created_at          AS "createdAt",
            o.updated_at          AS "updatedAt",
            
            da.name               AS "deliveryAgentName",
            da.mobile_no          AS "deliveryAgentMobileNo",
            da.current_status     AS "deliveryAgentCurrentStatus",
            da.created_at         AS "deliveryAgentCreatedAt",
            da.last_active_at     AS "deliveryAgentLastActiveAt",
                
            s.subscription_id     AS "subscriptionId",
            s.delivery_type       AS "subscriptionDeliveryType",
            s.plan_type           AS "subscriptionPlanType",
            s.is_active           AS "subscriptionIsActive",
            s.price               AS "subscriptionPrice",
            s.start_date          AS "subscriptionStartDate",
            s.end_date            AS "subscriptionEndData",
            s.created_at          AS "subscriptionCreatedAt",
            s.updated_at          AS "subscriptionUpdatedAt",
    
            u.user_id             AS "userId",
            u.name                AS "userName",
            u.address             AS "userAddress",
            u.created_at           AS "userCreatedAt",
            u.updated_at           AS "userUpdatedAt",
            
            m.menu_id             AS "menuId",
            m.items               AS "menuItems",
            m.chef_id             AS "menuChefId",
            m.week_day            AS "menuWeekDay",
            m.thumbnail_url       AS "menuThumbnailUrl",
            m.meal_type           AS "menuMealType",
            m.is_active           AS "menuIsActive",
            m.created_at          AS "menuCreatedAt",
            m.updated_at          AS "menuUpdatedAt"
        FROM orders o
        LEFT JOIN delivery_agents da
            ON da.delivery_agent_id = o.delivery_agent_id
        JOIN subscriptions s
            ON s.subscription_id = o.subscription_id
        JOIN users u
            ON u.user_id = s.user_id
        JOIN meal_plans mp
            ON mp.meal_plan_id = s.meal_plan_id
        JOIN chefs c
            ON c.chef_id = mp.chef_id
        JOIN menus m
            ON m.chef_id = c.chef_id AND m.is_active = TRUE
        WHERE
            s.is_active = TRUE AND 
            c.chef_id = :chefId AND
            o.order_date::date = CURRENT_DATE AND
            m.week_day = UPPER(TO_CHAR(CURRENT_DATE, 'FMDay')) AND
            m.meal_type = mp.meal_type;
    """)
    List<OrderForChefProjection> findByChefId(UUID chefId);
}

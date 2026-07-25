package com.example.tds.repository;

import com.example.tds.enums.OrderStatus;
import com.example.tds.entity.OrderRouteEntity;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface OrderRouteRepository extends JpaRepository<OrderRouteEntity, UUID> {
    Optional<OrderRouteEntity> findByOrder_OrderIdAndStatusFor(UUID orderId, OrderStatus statusFor);
}

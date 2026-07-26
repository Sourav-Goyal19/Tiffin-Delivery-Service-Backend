package com.example.tds.repository;

import com.example.tds.entity.DeliveryAgentEarningEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryAgentEarningRepository extends JpaRepository<DeliveryAgentEarningEntity, UUID> {
    Optional<DeliveryAgentEarningEntity> findByDeliveryAgent_DeliveryAgentId(UUID deliveryAgentId);
}

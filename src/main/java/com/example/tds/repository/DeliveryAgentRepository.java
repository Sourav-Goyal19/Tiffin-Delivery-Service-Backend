package com.example.tds.repository;

import com.example.tds.entity.DeliveryAgentEntity;
import com.example.tds.enums.DeliveryAgentCurrentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgentEntity, UUID> {
    Optional<DeliveryAgentEntity> findByMobileNo(String mobileNo);
    Optional<DeliveryAgentEntity> findByDeliveryAgentId(UUID deliveryAgentId);

    List<DeliveryAgentEntity> findByDeliveryAgentIdInAndCurrentStatus(List<UUID> deliveryAgentIds, DeliveryAgentCurrentStatus currentStatus);
}

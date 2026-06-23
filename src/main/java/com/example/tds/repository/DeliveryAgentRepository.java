package com.example.tds.repository;

import com.example.tds.entity.DeliveryAgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgentEntity, UUID> {
    Optional<DeliveryAgentEntity> findByMobileNo(String mobileNo);
    Optional<DeliveryAgentEntity> findByDeliveryAgentId(UUID deliveryAgentId);

    @Transactional
    @Modifying
    @Query(value = "update delivery_agents set location = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) where delivery_agent_id = :id", nativeQuery = true)
    int updateLocation(@Param("id") UUID id, @Param("lng") double longitude, @Param("lat") double latitude);
}

package com.example.tds.repository;

import com.example.tds.entity.DeliveryPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveryPaymentRepository extends JpaRepository<DeliveryPaymentEntity, UUID> {
}

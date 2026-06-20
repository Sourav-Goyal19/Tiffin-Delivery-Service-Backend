package com.example.tds.repository;

import com.example.tds.entity.BillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<BillEntity, UUID> {
    Optional<BillEntity> findByBillId(UUID billId);
}

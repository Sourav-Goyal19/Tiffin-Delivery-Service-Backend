package com.example.tds.repository;

import com.example.tds.entity.ChefPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChefPaymentRepository extends JpaRepository<ChefPaymentEntity, UUID> {

}

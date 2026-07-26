package com.example.tds.repository;

import com.example.tds.entity.ChefEarningEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChefEarningRepository extends JpaRepository<ChefEarningEntity, UUID> {
}

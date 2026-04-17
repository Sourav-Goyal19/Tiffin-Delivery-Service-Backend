package com.example.tds.repository;

import com.example.tds.entity.ChefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface ChefRepository extends JpaRepository<ChefEntity, UUID> {
    Optional<ChefEntity> findByMobileNo(String mobileNo);
    Optional<ChefEntity> findByChefId(UUID chefId);

    @Transactional
    @Modifying
    @Query(value = "update chefs set location = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), address=:address where chef_id = :id", nativeQuery = true)
    int updateLocation(@Param("id") UUID id, @Param("address") String address, @Param("lng") double longitude, @Param("lat") double latitude);
}

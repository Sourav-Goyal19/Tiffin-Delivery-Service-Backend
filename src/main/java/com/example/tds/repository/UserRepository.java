package com.example.tds.repository;

import com.example.tds.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByMobileNo(String mobileNo);

    @Modifying
    @Query(value = "update users set location = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), address=:address where user_id = :id", nativeQuery = true)
    int updateLocation(@Param("id") UUID id, @Param("address") String address, @Param("lng") double longitude, @Param("lat") double latitude);
}

package com.example.tds.repository;

import com.example.tds.entity.ChefEntity;
import org.springframework.data.jpa.repository.Query;
import com.example.tds.projection.ChefSearchProjection;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ChefRepository extends JpaRepository<ChefEntity, UUID> {
    Optional<ChefEntity> findByMobileNo(String mobileNo);
    Optional<ChefEntity> findByChefId(UUID chefId);

    @Modifying
    @Query(value = "update chefs set location = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), address=:address where chef_id = :id", nativeQuery = true)
    void updateLocation(@Param("id") UUID id, @Param("address") String address, @Param("lng") double longitude, @Param("lat") double latitude);

    @Query(value = """
        SELECT
            c.chef_id                          AS "chefId",
            c.name                             AS "name",
            c.address                          AS "address",
            c.mobile_no                        AS "mobileNo",
            c.avatar_url                       AS "avatarUrl",
            c.rating                           AS "rating",
            ST_X(c.location::geometry)         AS "longitude",
            ST_Y(c.location::geometry)         AS "latitude",
            ROUND((ST_Distance(c.location::geography, u.location::geography) / 1000.0)::numeric, 2)
                                               AS "disInKm",
            c.created_at                       AS "createdAt",
            c.updated_at                       AS "updatedAt",
            CASE
                WHEN c.name = :trimmedQuery                         THEN 1
                WHEN c.name ILIKE :escapedPrefix  ESCAPE '\\'       THEN 2
                WHEN c.name ILIKE :escapedSubstring ESCAPE '\\'     THEN 3
                ELSE 4
            END                                AS "nameMatchPriority"
        FROM chefs c
        JOIN users u ON u.user_id = :userId
        WHERE u.location IS NOT NULL
          AND ST_DWithin(c.location, u.location, 5000)
          AND (
                c.name    ILIKE :escapedSubstring ESCAPE '\\'
             OR c.address ILIKE :escapedSubstring ESCAPE '\\'
          )
          AND EXISTS (
                SELECT 1 FROM menus m
                WHERE m.chef_id = c.chef_id AND m.is_active = TRUE
          )
          AND EXISTS (
                SELECT 1 FROM meal_plans mp
                WHERE mp.chef_id = c.chef_id
                  AND mp.is_active = TRUE
                  AND (mp.remaining_capacity IS NOT NULL AND mp.remaining_capacity > 0)
          )
        ORDER BY "nameMatchPriority" ASC, c.name ASC, c.address ASC
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<ChefSearchProjection> searchChefs(
            @Param("userId") UUID userId,
            @Param("trimmedQuery") String trimmedQuery,
            @Param("escapedPrefix") String escapedPrefix,
            @Param("escapedSubstring") String escapedSubstring,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM chefs c
        JOIN users u ON u.user_id = :userId
        WHERE u.location IS NOT NULL
          AND ST_DWithin(c.location, u.location, 5000)
          AND (
                c.name    ILIKE :escapedSubstring ESCAPE '\\'
             OR c.address ILIKE :escapedSubstring ESCAPE '\\'
          )
          AND EXISTS (
                SELECT 1 FROM menus m
                WHERE m.chef_id = c.chef_id AND m.is_active = TRUE
          )
          AND EXISTS (
                SELECT 1 FROM meal_plans mp
                WHERE mp.chef_id = c.chef_id
                  AND mp.is_active = TRUE
                  AND (mp.remaining_capacity IS NOT NULL AND mp.remaining_capacity > 0)
          )
    """, nativeQuery = true)
    long countSearchChefs(
            @Param("userId") UUID userId,
            @Param("escapedSubstring") String escapedSubstring
    );
}

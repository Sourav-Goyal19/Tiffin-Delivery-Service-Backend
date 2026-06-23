package com.example.tds.entity;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChefEntity {
    @Id
    @Column(name = "chef_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID chefId;

    @Column(name = "name")
    private String name;

    @Column(name = "mobile_no", nullable = false, length = 10, unique = true)
    private String mobileNo;

    @Column(name = "otp", length = 6)
    private Integer otp;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "address")
    private String address;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(name = "rating")
    private Double rating;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

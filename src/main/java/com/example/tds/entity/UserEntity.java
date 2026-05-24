package com.example.tds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "name", nullable = true)
    private String name;

    @Column(name = "mobile_no", length = 10, unique = true, nullable = false)
    private String mobileNo;

    @Column(name = "otp", length = 6)
    private Integer otp;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(name = "address")
    private String address;
}

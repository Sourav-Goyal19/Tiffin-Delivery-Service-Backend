package com.example.tds.entity;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.CreationTimestamp;
import com.example.tds.enums.DeliveryAgentCurrentStatus;

import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_agents")
@Getter
@Setter
public class DeliveryAgentEntity {
    @Id
    @Column(name = "delivery_agent_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID deliveryAgentId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "mobile_no", nullable = false, length = 10, unique = true)
    private String mobileNo;

    @Column(name = "otp", length = 4)
    private Integer otp;

    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(name = "current_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliveryAgentCurrentStatus currentStatus = DeliveryAgentCurrentStatus.OFFLINE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_active_at", nullable = false)
    private LocalDateTime lastActiveAt;

    @Column(name = "fcm_token")
    private String fcmToken;
}

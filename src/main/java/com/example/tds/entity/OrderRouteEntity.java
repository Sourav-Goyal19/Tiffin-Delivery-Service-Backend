package com.example.tds.entity;

import com.example.tds.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.example.tds.dto.responses.RouteInstructionsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRouteEntity {
    @Id
    @Column(name = "order_route_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderRouteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "encoded_polyline", columnDefinition = "TEXT", nullable = false)
    private String encodedPolyline;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds;

    @Column(name = "instructions", columnDefinition = "TEXT")
    @Convert(converter = RouteInstructionsConverter.class)
    private List<RouteInstructionsDto> instructions;

    @Column(name = "status_for", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus statusFor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

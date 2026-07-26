package com.example.tds.entity;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import com.example.tds.enums.OrderStatus;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.CreationTimestamp;

import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class OrderEntity {
    @Id
    @Column(name = "order_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="subscription_id", referencedColumnName = "subscription_id", nullable = false)
    private SubscriptionEntity subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="delivery_agent_id", referencedColumnName = "delivery_agent_id")
    private DeliveryAgentEntity deliveryAgent;

    @Column(name = "from_location", nullable = false)
    private String fromLocation;

    @Column(name = "to_location", nullable = false)
    private String toLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "pickup_otp", nullable = false)
    private Integer pickUpOtp = 1234;

    @Column(name = "drop_otp", nullable = false)
    private Integer dropOtp;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

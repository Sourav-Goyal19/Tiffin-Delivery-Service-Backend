package com.example.tds.entity;

import lombok.Data;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.CreationTimestamp;

import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_agents_earnings")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryAgentEarningEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "delivery_agent_earning_id")
    private UUID deliveryAgentEarningId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_agent_id", referencedColumnName = "delivery_agent_id", nullable = false)
    private DeliveryAgentEntity deliveryAgent;

    @Column(name = "earning", nullable = false)
    private Double earning;

    @Column(name = "withdraw", nullable = false)
    private Double withdraw;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

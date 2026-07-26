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
@Table(name = "chef_earnings")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChefEarningEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chef_earning_id")
    private UUID deliveryAgentEarningId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_id", referencedColumnName = "chef_id", nullable = false)
    private ChefEntity chef;

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

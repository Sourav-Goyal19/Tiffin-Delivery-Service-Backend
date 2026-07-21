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
@Table(name = "chef_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChefPaymentEntity {
    @Id
    @Column(name = "chef_payment_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID chefPaymentId;

    @ManyToOne
    @JoinColumn(name = "chef_id", referencedColumnName = "chef_id", nullable = false)
    private ChefEntity chef;

    @OneToOne
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false, unique = true)
    private OrderEntity order;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

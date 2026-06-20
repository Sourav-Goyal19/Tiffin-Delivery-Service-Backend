package com.example.tds.entity;

import com.example.tds.enums.PaymentVia;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
public class PaymentEntity {
    @Id
    @Column(name = "payment_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentId;

    @Column(name = "razorpay_payment_id", nullable = false)
    private String razorpayPaymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", referencedColumnName = "bill_id", nullable = false)
    private BillEntity bill;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "payment_via", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentVia paymentVia;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

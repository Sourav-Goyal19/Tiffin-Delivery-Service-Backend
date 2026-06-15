package com.example.tds.entity;

import com.example.tds.enums.MealType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "meal_plans")
@Getter
@Setter
public class MealPlanEntity {
    @Id
    @Column(name = "meal_plan_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID mealPlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_id", referencedColumnName = "chef_id", nullable = false)
    private ChefEntity chef;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    @Column(name = "weekly_price")
    private Double weeklyPrice;

    @Column(name = "monthly_price")
    private Double monthlyPrice;

    @Column(name = "timing", nullable = false)
    private LocalTime timing;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void validatePrices() {
        if (weeklyPrice == null && monthlyPrice == null) {
            throw new IllegalStateException("Either weekly_price or monthly_price is null");
        }

        if(weeklyPrice != null && weeklyPrice <= 0){
            throw new IllegalArgumentException("Weekly price can't be less than or equal to 0");
        }

        if(monthlyPrice != null && monthlyPrice <= 0){
            throw new IllegalArgumentException("Monthly price can't be less than or equal to 0");
        }
    }
}

package com.example.tds.entity;

import lombok.*;
import jakarta.persistence.*;
import com.example.tds.enums.WeekDay;
import com.example.tds.enums.MealType;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.CreationTimestamp;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "menus")
@Getter
@Setter
public class MenuEntity {
    @Id
    @Column(name = "menu_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID menuId;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    @Column(name = "items", nullable = false)
    private List<String> items;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "is_active", nullable = false)
    @ColumnDefault("false")
    private Boolean isActive;

    @Column(name = "capacity")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_day", nullable = false)
    private WeekDay weekDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_id", referencedColumnName = "chef_id", nullable = false)
    private ChefEntity chef;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

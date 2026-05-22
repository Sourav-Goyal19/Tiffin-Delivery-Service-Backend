package com.example.tds.entity;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.type.SqlTypes;
import com.example.tds.enums.WeekDay;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Map;
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", columnDefinition = "jsonb")
    private Map<String, Object> items;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_day")
    private WeekDay weekDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_id", referencedColumnName = "chef_id")
    private ChefEntity chef;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

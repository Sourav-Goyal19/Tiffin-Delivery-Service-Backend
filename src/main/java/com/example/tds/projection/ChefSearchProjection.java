package com.example.tds.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ChefSearchProjection {
    UUID getChefId();
    String getName();
    String getAddress();
    String getMobileNo();
    String getAvatarUrl();
    Double getRating();
    Double getLongitude();
    Double getLatitude();
    Double getDisInKm();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    Integer getNameMatchPriority();
}
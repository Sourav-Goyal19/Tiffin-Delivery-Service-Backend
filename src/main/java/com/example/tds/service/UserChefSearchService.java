package com.example.tds.service;

import com.example.tds.dto.responses.ChefResponse;
import com.example.tds.dto.responses.PaginationResponse;
import com.example.tds.entity.UserEntity;
import com.example.tds.exception.UnauthorizedException;
import com.example.tds.projection.ChefSearchProjection;
import com.example.tds.repository.ChefRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserChefSearchService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final ChefRepository chefRepository;

    public Map<String, Object> handleSearchChefs(
            UUID userId, UserEntity currentUser, String q, Integer page, Integer limit
    ) {
        if (currentUser == null || !userId.equals(currentUser.getId())) {
            throw new UnauthorizedException("User not authorized");
        }

        int p = (page == null) ? DEFAULT_PAGE : Math.max(1, page);
        int l = (limit == null) ? DEFAULT_LIMIT
                                 : Math.min(MAX_LIMIT, Math.max(1, limit));
        int offset = (p - 1) * l;

        String trimmed = q.trim();
        log.debug("Searching chefs for userId: {}, query: '{}', page: {}, limit: {}", userId, trimmed, p, l);

        String escapedPrefix = escapeLike(trimmed) + "%";
        String escapedSubstring = "%" + escapeLike(trimmed) + "%";

        // TODO: Collapse COUNT(*) and SELECT into a single query using COUNT(*) OVER() if performance/network round-trips become an issue
        long total = chefRepository.countSearchChefs(userId, escapedSubstring);
        List<ChefSearchProjection> rows = chefRepository.searchChefs(
                userId, trimmed, escapedPrefix, escapedSubstring, l, offset
        );

        List<ChefResponse> chefs = rows.stream().map(this::toChefResponse).toList();

        int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / l);
        PaginationResponse pagination = PaginationResponse.builder()
                .page(p).limit(l).total(total).totalPages(totalPages)
                .build();

        return Map.of("chefs", chefs, "pagination", pagination);
    }

    private ChefResponse toChefResponse(ChefSearchProjection p) {
        return ChefResponse.builder()
                .chefId(p.getChefId())
                .name(p.getName())
                .address(p.getAddress())
                .mobileNo(p.getMobileNo())
                .avatarUrl(p.getAvatarUrl())
                .rating(p.getRating())
                .longitude(p.getLongitude())
                .latitude(p.getLatitude())
                .disInKm(p.getDisInKm())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    /** Escape LIKE wildcards. Backslash must be escaped FIRST to avoid double-escaping. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
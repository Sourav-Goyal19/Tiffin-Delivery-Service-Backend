package com.example.tds.interceptors;

import com.example.tds.entity.ChefEntity;
import com.example.tds.entity.UserEntity;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.repository.ChefRepository;
import com.example.tds.repository.UserRepository;
import com.example.tds.utilities.JwtUtility;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ChefAuthInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(UserAuthInterceptor.class);
    @Autowired
    private JwtUtility jwtUtil;
    @Autowired
    private ChefRepository chefRepository;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Object handler) throws Exception{
//        log.info("Passed from auth middleware");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String authHeader = request.getHeader("Authorization");
        String accessToken = null;

        if(authHeader != null && authHeader.startsWith("Bearer ")){
            accessToken = authHeader.substring(7);
        }

        if (accessToken == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{" +
                    "\"error\": \"Missing access token\",\n" +
                    "\"success\": \"false\"" +
                    "}");
            return false;
        }

        if (!jwtUtil.validateToken(accessToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{" +
                    "\"error\": \"Invalid or expired token\",\n" +
                    "\"success\": \"false\"" +
                    "}");
            return false;
        }

        Claims claims = jwtUtil.extractAllClaims(accessToken);
        String mobileNo = claims.get("mobileNo", String.class);

        ChefEntity chefEntity = chefRepository.findByMobileNo(mobileNo)
                .orElseThrow(()->new ResourceNotFoundException("Chef not found"));

        request.setAttribute("chef", chefEntity);
        return true;
    }
}

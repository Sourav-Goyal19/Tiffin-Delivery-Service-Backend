package com.example.tds.interceptors;

import com.example.tds.entity.UserEntity;
import com.example.tds.repository.UserRepository;
import com.example.tds.service.LogoutService;
import com.example.tds.utilities.JwtUtility;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserAuthInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtility jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LogoutService logoutService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
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
                    "\"message\": \"Missing access token\",\n" +
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

        if (logoutService.isAccessTokenRevoked(accessToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{" +
                    "\"error\": \"Invalid or expired token\",\n" +
                    "\"success\": \"false\"" +
                "}");
            return false;
        }

        Claims claims = jwtUtil.extractAllClaims(accessToken);
        String mobileNo = claims.get("mobileNo", String.class);

        UserEntity userEntity = userRepository.findByMobileNo(mobileNo).orElse(null);
        if (userEntity == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{" +
                    "\"error\": \"User not found\",\n" +
                    "\"success\": \"false\"" +
                "}");
            return false;
        }

        request.setAttribute("user", userEntity);
        return true;
    }
}
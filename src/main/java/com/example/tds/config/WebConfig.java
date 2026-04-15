package com.example.tds.config;

import com.example.tds.interceptors.AuthInterceptor;
import com.example.tds.interceptors.LogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
   @Autowired
   private AuthInterceptor authInterceptor;
   @Autowired
   private LogInterceptor logInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        List.of(
                                "/api/users/login",
                                "/api/users/signup",
                                "/api/users/refresh"
                        )
                );

        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/api/**");
    }
}

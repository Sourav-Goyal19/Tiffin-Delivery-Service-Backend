package com.example.tds.config;

import com.example.tds.interceptors.ChefAuthInterceptor;
import com.example.tds.interceptors.UserAuthInterceptor;
import com.example.tds.interceptors.LogInterceptor;
import com.example.tds.interceptors.DeliveryAgentAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
   @Autowired
   private UserAuthInterceptor userAuthInterceptor;
   @Autowired
   private LogInterceptor logInterceptor;
   @Autowired
   private ChefAuthInterceptor chefAuthInterceptor;
   @Autowired
   private DeliveryAgentAuthInterceptor deliveryAgentAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(userAuthInterceptor)
                .addPathPatterns("/api/users/**")
                .excludePathPatterns(
                        List.of(
                                "/api/users/otp/generate",
                                "/api/users/otp/verify",
                                "/api/users/signup",
                                "/api/users/refresh",
                                "/api/users/*/location"
                        )
                );

        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/api/**");

        registry.addInterceptor(chefAuthInterceptor)
                .addPathPatterns("/api/chefs/**")
                .excludePathPatterns(
                        List.of(
                                "/api/chefs/otp/generate",
                                "/api/chefs/otp/verify",
                                "/api/chefs/signup",
                                "/api/chefs/refresh",
                                "/api/chefs/*/location"
                        )
                );

        registry.addInterceptor(deliveryAgentAuthInterceptor)
                .addPathPatterns("/api/delivery-agents/**")
                .excludePathPatterns(
                        List.of(
                                "/api/delivery-agents/otp/generate",
                                "/api/delivery-agents/otp/verify",
                                "/api/delivery-agents/refresh",
                                "/api/delivery-agents/location",
                                "/api/delivery-agents/{agentId}/location/{orderId}"
                        )
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

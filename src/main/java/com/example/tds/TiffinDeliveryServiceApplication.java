package com.example.tds;

import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(info = @Info(title = "TDS-Backend", version = "1.0"))
public class TiffinDeliveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiffinDeliveryServiceApplication.class, args);
    }

}

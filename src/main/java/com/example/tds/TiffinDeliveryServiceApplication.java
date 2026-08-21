package com.example.tds;

import io.github.cdimascio.dotenv.Dotenv;
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
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry ->
            System.setProperty(entry.getKey(), entry.getValue())
        );

        SpringApplication.run(TiffinDeliveryServiceApplication.class, args);
    }

}

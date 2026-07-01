package com.example.tds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.UUID;

@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        // :-: RedisConnectionFactory creates a connection with running redis instance through variables defined in our application.properties, and then we use that connection in our application. :-:
        // :-: RedisMessageListener is a container which manages all our different message listeners. It creates a separate thread that doesn't stop(until we stop) and listens all our upcoming messages over there. :-:
        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        // :-: Adding listener to our connection. :-:
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    public RedisTemplate<String, UUID> redisTemplate(RedisConnectionFactory connectionFactory) {
        // :-: here RedisTemplate is a global object using which we execute all redis commands/operations like we are using opsForGeo for geo operations, others can be opsForValues for key-value pairs for caching, opsForHash, opsForZSet, etc :-:
        RedisTemplate<String, UUID> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public GeoOperations<String, UUID> geoOperations(RedisTemplate<String, UUID> template) {
        return template.opsForGeo();
    }
}

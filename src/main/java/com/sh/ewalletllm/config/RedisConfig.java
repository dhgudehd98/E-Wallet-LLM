package com.sh.ewalletllm.config;

import com.sh.ewalletllm.redis.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;  // ✅ 수정
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public ReactiveRedisTemplate<String, ChatMessageDto> chatRedisTemplate(
            ReactiveRedisConnectionFactory factory
    ) {
        ObjectMapper mapper = new ObjectMapper();

        Jackson2JsonRedisSerializer<ChatMessageDto> serializer =
                new Jackson2JsonRedisSerializer<>(mapper, ChatMessageDto.class);

        RedisSerializationContext<String, ChatMessageDto> context =
                RedisSerializationContext.<String, ChatMessageDto>newSerializationContext(new StringRedisSerializer())
                        .value(serializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
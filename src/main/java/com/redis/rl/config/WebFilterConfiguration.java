package com.redis.rl.config;

import com.redis.rl.filter.RateLimiterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.server.WebFilter;

import java.util.Collections;

@Configuration
public class WebFilterConfiguration {

    @Bean
    WebFilter rateLimiterFilter(ReactiveRedisTemplate<String, Long> redisTemplate) {
        return new RateLimiterFilter(redisTemplate, Collections.emptyList(), Collections.singletonList("/rate-limit/ping"));
    }

    @Bean
    ReactiveRedisTemplate<String, Long> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        JdkSerializationRedisSerializer jdkSerializationRedisSerializer = new JdkSerializationRedisSerializer();
        StringRedisSerializer stringRedisSerializer = StringRedisSerializer.UTF_8;
        GenericToStringSerializer<Long> longToStringSerializer = new GenericToStringSerializer<>(Long.class);
        ReactiveRedisTemplate<String, Long> template = new ReactiveRedisTemplate<>(factory,
                RedisSerializationContext.<String, Long>newSerializationContext(jdkSerializationRedisSerializer)
                        .key(stringRedisSerializer).value(longToStringSerializer).build());
        return template;
    }
}

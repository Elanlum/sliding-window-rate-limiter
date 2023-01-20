package com.elanlum.rl.config;

import com.elanlum.rl.filter.RateLimiterFilterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.server.WebFilter;

import java.util.List;

@Configuration
public class WebFilterConfiguration {

    @Value("${rateLimit.windowTimeMs:60000}")
    private Long windowTime;

    @Value("${rateLimit.maxRequestAmount:5}")
    private Long maxRequestAmount;

    @Value("${rateLimit.includePaths:}")
    private List<String> includePaths;

    @Value("${rateLimit.excludePaths:}")
    private List<String> excludePaths;



    @Bean
    WebFilter rateLimiterFilter(ReactiveRedisTemplate<String, Long> redisTemplate) {
        return RateLimiterFilterBuilder.builder()
                .withRedisTemplate(redisTemplate)
                .withIncludePaths(includePaths)
                .withExcludePaths(excludePaths)
                .withMaxRequests(maxRequestAmount)
                .withWindowTime(windowTime)
                .build();
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

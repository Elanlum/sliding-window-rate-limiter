package com.elanlum.rl.filter;

import org.springframework.data.redis.core.ReactiveRedisTemplate;

import java.util.List;

public class RateLimiterFilterBuilder {

    private final RateLimiterFilter filter;

    public static RateLimiterFilterBuilder builder() {
        return new RateLimiterFilterBuilder();
    }

    private RateLimiterFilterBuilder() {
        this.filter = new RateLimiterFilter();
    }

    public RateLimiterFilterBuilder withRedisTemplate(ReactiveRedisTemplate<String, Long> redisTemplate) {
        this.filter.setRedisTemplate(redisTemplate);
        return this;
    }

    public RateLimiterFilterBuilder withMaxRequests(Long maxRequests) {
        this.filter.setMaxRequestsPerWindow(maxRequests);
        return this;
    }

    public RateLimiterFilterBuilder withWindowTime(Long windowTime) {
        this.filter.setSlidingWindowMs(windowTime);
        return this;
    }

    public RateLimiterFilterBuilder withExcludePaths(List<String> excludePaths) {
        this.filter.setExcludePaths(excludePaths);
        return this;
    }

    public RateLimiterFilterBuilder withIncludePaths(List<String> includePaths) {
        this.filter.setIncludePaths(includePaths);
        return this;
    }

    public RateLimiterFilter build() {
        return filter;
    }

}

package com.elanlum.rl.filter;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

public class RateLimiterFilter implements WebFilter {

    private ReactiveRedisTemplate<String, Long> redisTemplate;

    private Long maxRequestsPerWindow;
    private Long slidingWindowMs;
    private List<String> excludePaths;
    private List<String> includePaths;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public void setRedisTemplate(ReactiveRedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setMaxRequestsPerWindow(Long maxRequestsPerWindow) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
    }

    public void setSlidingWindowMs(Long slidingWindowMs) {
        this.slidingWindowMs = slidingWindowMs;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public void setIncludePaths(List<String> includePaths) {
        this.includePaths = includePaths;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (skip(path)) {
            return chain.filter(exchange);
        }

        String key = "rate_limit";
        ByteBuffer bbKey = ByteBuffer.wrap(key.getBytes());

        return redisTemplate.createMono(connection -> {
            long currentTime = System.currentTimeMillis();

            Mono<Tuple3<Long, Long, Boolean>> removeByScore = Mono.zip(
                    connection.zSetCommands().zRemRangeByScore(bbKey, Range.from(Range.Bound.inclusive(0.0))
                            .to(Range.Bound.inclusive(Double.parseDouble(String.valueOf(currentTime)) - Double.parseDouble(String.valueOf(slidingWindowMs))))),
                    connection.zSetCommands().zAdd(bbKey, Double.valueOf(String.valueOf(currentTime)), ByteBuffer.wrap(Long.toString(currentTime).getBytes())),
                    connection.keyCommands().expire(bbKey, Duration.ofMillis(currentTime + slidingWindowMs))
            );

            Mono<List<ByteBuffer>> countRequests = removeByScore.then(connection.zSetCommands()
                    .zRange(bbKey, Range.from(Range.Bound.inclusive(0L)).to(Range.Bound.inclusive(-1L))).collectList());

            return countRequests.flatMap(list -> {
                if (list.size() > maxRequestsPerWindow) {
                    exchange.getResponse().setStatusCode(TOO_MANY_REQUESTS);
                    return Mono.empty();
                }

                return Mono.empty().then(chain.filter(exchange));
            });
        });
    }

    private boolean skip(String path) {
        return !isPathMatches(path, includePaths) || isPathMatches(path, excludePaths);
    }

    private boolean isPathMatches(String path, List<String> patterns) {
        for (String pattern : patterns) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}

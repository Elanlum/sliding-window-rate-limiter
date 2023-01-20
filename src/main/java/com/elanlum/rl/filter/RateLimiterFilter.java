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

    private static final Long MAX_REQUESTS_PER_MINUTE = 5L;
    private final ReactiveRedisTemplate<String, Long> redisTemplate;
    private final List<String> excludePaths;
    private final List<String> includePaths;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimiterFilter(
            ReactiveRedisTemplate<String, Long> redisTemplate,
            List<String> excludePaths,
            List<String> includePaths
    ) {
        this.redisTemplate = redisTemplate;
        this.excludePaths = excludePaths;
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
            long slidingWindowTime = 60000L;

            Mono<Tuple3<Long, Long, Boolean>> removeByScore = Mono.zip(
                    connection.zSetCommands().zRemRangeByScore(bbKey, Range.from(Range.Bound.inclusive(0.0))
                            .to(Range.Bound.inclusive(Double.parseDouble(String.valueOf(currentTime)) - Double.parseDouble(String.valueOf(slidingWindowTime))))),
                    connection.zSetCommands().zAdd(bbKey, Double.valueOf(String.valueOf(currentTime)), ByteBuffer.wrap(Long.toString(currentTime).getBytes())),
                    connection.keyCommands().expire(bbKey, Duration.ofMillis(currentTime + slidingWindowTime))
            );

            Mono<List<ByteBuffer>> countRequests = removeByScore.then(connection.zSetCommands()
                    .zRange(bbKey, Range.from(Range.Bound.inclusive(0L)).to(Range.Bound.inclusive(-1L))).collectList());

            return countRequests.flatMap(list -> {
                if (list.size() > MAX_REQUESTS_PER_MINUTE) {
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

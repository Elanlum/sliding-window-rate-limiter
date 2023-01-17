package com.redis.rl;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

public class RateLimiterFilter implements WebFilter {

    private static final Long MAX_REQUESTS_PER_MINUTE = 5L;
    private final ReactiveRedisTemplate<String, Long> redisTemplate;

    public RateLimiterFilter(ReactiveRedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String key = "rate_limit_";
        ByteBuffer bbKey = ByteBuffer.wrap(key.getBytes());

        redisTemplate.createMono(connection -> {
            long currentTime = System.currentTimeMillis();
            long slidingWindowTime = 60000L;

            Mono<Long> removeByScore = connection.zSetCommands().zRemRangeByScore(bbKey, Range.from(Range.Bound.inclusive(0.0))
                    .to(Range.Bound.inclusive(Double.parseDouble(String.valueOf(currentTime)) - Double.parseDouble(String.valueOf(slidingWindowTime)))));

            Mono<List<ByteBuffer>> countRequests = removeByScore.then(connection.zSetCommands()
                    .zRange(bbKey, Range.from(Range.Bound.inclusive(0L)).to(Range.Bound.inclusive(-1L))).collectList());

            return countRequests.flatMap(list -> {
                if (list.size() >= MAX_REQUESTS_PER_MINUTE) {
                    return ServerResponse.status(TOO_MANY_REQUESTS).build();
                }

                return Mono.zip(
                        connection.zSetCommands().zAdd(bbKey, Double.valueOf(String.valueOf(currentTime)), ByteBuffer.wrap(Long.toString(currentTime).getBytes())),
                        connection.keyCommands().expire(bbKey, Duration.ofMillis(currentTime + slidingWindowTime))
                );
            });
        });
        return chain.filter(exchange);
    }

//    private String requestAddress(Optional<InetSocketAddress> maybeAddress) {
//        return maybeAddress.isPresent() ? maybeAddress.get().getHostName() : "";
//    }
}

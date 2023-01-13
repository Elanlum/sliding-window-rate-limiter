package com.redis.rl;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

public class RateLimiterHandlerFilterFunction implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final Long MAX_REQUESTS_PER_MINUTE = 5L;
    private final ReactiveRedisTemplate<String, Long> redisTemplate;

    public RateLimiterHandlerFilterFunction(ReactiveRedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        String key = String.format("rate_limit_%s", requestAddress(request.remoteAddress()));

        Flux<Long> flux = function(key);
        Mono<Long> mono = flux.next();

        return mono.flatMap(a -> {
            if (a > MAX_REQUESTS_PER_MINUTE) {
                return ServerResponse.status(TOO_MANY_REQUESTS).build();
            }
            return next.handle(request);
        });
    }

    private Flux<Long> function(String key) {

        return redisTemplate.execute(connection -> {
            long currentTime = System.currentTimeMillis();
            long slidingWindowTime = 60000L;

            ByteBuffer bbKey = ByteBuffer.wrap(key.getBytes());

            Mono<Tuple4<Long, Long, Boolean, List<ByteBuffer>>> zip = Mono.zip(
                    connection.zSetCommands().zRemRangeByScore(bbKey, Range.from(Range.Bound.inclusive(0.0))
                            .to(Range.Bound.inclusive(Double.parseDouble(String.valueOf(currentTime)) - Double.parseDouble(String.valueOf(slidingWindowTime))))),
                    connection.zSetCommands().zAdd(bbKey, Double.valueOf(String.valueOf(currentTime)), ByteBuffer.wrap(Long.toString(currentTime).getBytes())),
                    connection.keyCommands().expire(bbKey, Duration.ofMillis(currentTime + slidingWindowTime)),
                    connection.zSetCommands().zRange(bbKey, Range.from(Range.Bound.inclusive(0L)).to(Range.Bound.inclusive(-1L))).collectList()
            );
            return zip.map(tuples -> (long) tuples.getT4().size());
        });
    }

    private String requestAddress(Optional<InetSocketAddress> maybeAddress) {
        return maybeAddress.isPresent() ? maybeAddress.get().getHostName() : "";
    }
}

package com.redis.rl.filter.function;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

public class FixedWindowRateLimiterHandlerFilterFunction implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final Long MAX_REQUESTS_PER_MINUTE = 5L;
    private final ReactiveRedisTemplate<String, Long> redisTemplate;

    public FixedWindowRateLimiterHandlerFilterFunction(ReactiveRedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        String key = String.format("rate_limit_%s", requestAddress(request.remoteAddress()));

        return redisTemplate
                .opsForValue().get(key)
                .flatMap(
                        value -> value >= MAX_REQUESTS_PER_MINUTE ?
                                ServerResponse.status(TOO_MANY_REQUESTS).build() :
                                incrAndExpireKey(key, request, next)
                ).switchIfEmpty(incrAndExpireKey(key, request, next));
    }

    private Mono<ServerResponse> incrAndExpireKey(String key, ServerRequest request, HandlerFunction<ServerResponse> next) {

        return redisTemplate.createMono(connection -> {

            ByteBuffer bbKey = ByteBuffer.wrap(key.getBytes());

            return Mono.zip(
                    connection.numberCommands().incr(bbKey),
                    connection.keyCommands().expire(bbKey, Duration.ofSeconds(59L))
            ).then(Mono.empty());

        }).then(next.handle(request));
    }

    private String requestAddress(Optional<InetSocketAddress> maybeAddress) {
        return maybeAddress.isPresent() ? maybeAddress.get().getHostName() : "";
    }
}

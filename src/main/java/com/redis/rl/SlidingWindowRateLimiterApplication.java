package com.redis.rl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class SlidingWindowRateLimiterApplication {

	@Bean
	RouterFunction<ServerResponse> routes(ReactiveRedisTemplate<String, Long> redisTemplate) {
		return route()
				.GET("/api/ping", r -> ok()
						.contentType(TEXT_PLAIN)
						.body(BodyInserters.fromValue("PONG"))
				).filter(new RateLimiterHandlerFilterFunction(redisTemplate)).build();
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

	public static void main(String[] args) {
		SpringApplication.run(SlidingWindowRateLimiterApplication.class, args);
	}

}

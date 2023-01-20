package com.elanlum.rl;

import com.elanlum.rl.filter.function.SlideWindowRateLimiterHandlerFilterFunction;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Configuration
@SpringBootApplication
public class SlidingWindowRateLimiterApplication {

	@Bean
	RouterFunction<ServerResponse> routes(ReactiveRedisTemplate<String, Long> redisTemplate) {
		return route()
				.GET("/api/ping", r -> ok()
						.contentType(TEXT_PLAIN)
						.body(BodyInserters.fromValue("PONG"))
				).filter(new SlideWindowRateLimiterHandlerFilterFunction(redisTemplate)).build();
	}

	public static void main(String[] args) {
		SpringApplication.run(SlidingWindowRateLimiterApplication.class, args);
	}

}

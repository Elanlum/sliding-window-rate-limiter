package com.elanlum.rl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/rate-limit")
public class TestController {

    @GetMapping("/ping")
    public Mono<String> getPing() {
        return Mono.just("PONG_NEW");
    }
}

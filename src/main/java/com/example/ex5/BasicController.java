package com.example.ex5;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class BasicController {

    @GetMapping("/api/welcome")
    public Mono<String> welcome() {
        return Mono.just("Welcome to Project Reactor!");
    }

    @GetMapping("/api/numbers")
    public Flux<Integer> numbers() {
        return Flux.range(1, 5);
    }
}


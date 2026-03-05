package com.example.ex8;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ArticleController {

    @GetMapping("/api/articles")
    public Flux<String> getArticles() {
        return Flux.just(
                "Introduction to Spring WebFlux",
                "Reactive Programming with Project Reactor",
                "Building APIs with Spring Boot"
        );
    }
}


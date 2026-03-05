package com.example.ex10;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class BookRouter {

    @Bean
    public RouterFunction<ServerResponse> bookRoutes(BookHandler handler) {
        return RouterFunctions.route()
                .GET("/api/books", handler::getAll)
                .GET("/api/books/search", handler::search)
                .POST("/api/books", handler::create)
                .DELETE("/api/books/{id}", handler::delete)
                .build();
    }
}


package com.example.ex9;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class TaskRouter {

    @Bean
    public RouterFunction<ServerResponse> taskRoutes(TaskHandler handler) {
        return RouterFunctions.route()
                .GET("/api/tasks", handler::getAll)
                .GET("/api/tasks/{id}", handler::getById)
                .POST("/api/tasks", handler::create)
                .PUT("/api/tasks/{id}", handler::update)
                .DELETE("/api/tasks/{id}", handler::delete)
                .build();
    }
}


package com.example.ex9;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskHandler {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    public Mono<ServerResponse> getAll(ServerRequest request) {
        return ServerResponse.ok().body(Flux.fromIterable(tasks.values()), Task.class);
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        String id = request.pathVariable("id");
        Task task = tasks.get(id);
        if (task == null) {
            return ServerResponse.notFound().build();
        }
        return ServerResponse.ok().bodyValue(task);
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(Task.class)
                .flatMap(task -> {
                    task.setId(UUID.randomUUID().toString());
                    if (task.getCompleted() == null) {
                        task.setCompleted(false);
                    }
                    tasks.put(task.getId(), task);
                    return ServerResponse.ok().bodyValue(task);
                });
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        String id = request.pathVariable("id");
        Task existing = tasks.get(id);
        if (existing == null) {
            return ServerResponse.notFound().build();
        }
        return request.bodyToMono(Task.class)
                .flatMap(task -> {
                    if (task.getDescription() != null) {
                        existing.setDescription(task.getDescription());
                    }
                    if (task.getCompleted() != null) {
                        existing.setCompleted(task.getCompleted());
                    }
                    tasks.put(id, existing);
                    return ServerResponse.ok().bodyValue(existing);
                });
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        String id = request.pathVariable("id");
        Task removed = tasks.remove(id);
        if (removed == null) {
            return ServerResponse.notFound().build();
        }
        return ServerResponse.ok().bodyValue(removed);
    }
}


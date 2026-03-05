package com.example.ex10;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BookHandler {

    private final Map<String, Book> books = new ConcurrentHashMap<>();

    public Mono<ServerResponse> getAll(ServerRequest request) {
        return ServerResponse.ok().body(Flux.fromIterable(books.values()), Book.class);
    }

    public Mono<ServerResponse> search(ServerRequest request) {
        String title = request.queryParam("title").orElse("");
        Flux<Book> result = Flux.fromIterable(books.values())
                .filter(book -> book.getTitle().toLowerCase().contains(title.toLowerCase()));
        return ServerResponse.ok().body(result, Book.class);
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(Book.class)
                .flatMap(book -> {
                    book.setId(UUID.randomUUID().toString());
                    books.put(book.getId(), book);
                    return ServerResponse.ok().bodyValue(book);
                });
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        String id = request.pathVariable("id");
        Book removed = books.remove(id);
        if (removed == null) {
            return ServerResponse.notFound().build();
        }
        return ServerResponse.ok().bodyValue(removed);
    }
}


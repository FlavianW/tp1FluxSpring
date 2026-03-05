package com.example.tp.repository;

import com.example.tp.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ProductRepository {

    private static final Duration LATENCY = Duration.ofMillis(100);
    private static final Random RANDOM = new Random();

    // Cache pour éviter les requêtes redondantes (Option B - Optimisation)
    private final Map<String, Product> cache = new ConcurrentHashMap<>();

    private final Map<String, Product> products = Map.of(
            "PROD001", new Product("PROD001", "Laptop", new BigDecimal("999.99"), 10, "electronique"),
            "PROD002", new Product("PROD002", "Smartphone", new BigDecimal("699.99"), 5, "electronique"),
            "PROD003", new Product("PROD003", "Chaise de bureau", new BigDecimal("249.99"), 20, "mobilier"),
            "PROD004", new Product("PROD004", "Clavier mécanique", new BigDecimal("129.99"), 0, "electronique"),
            "PROD005", new Product("PROD005", "Livre Java", new BigDecimal("39.99"), 50, "librairie")
    );

    public Mono<Product> findById(String id) {
        // Vérifier le cache d'abord
        Product cached = cache.get(id);
        if (cached != null) {
            return Mono.just(cached);
        }
        return Mono.justOrEmpty(products.get(id))
                .delayElement(LATENCY)
                .flatMap(this::maybeError)
                .doOnNext(product -> cache.put(id, product));
    }

    public Flux<Product> findByIds(List<String> ids) {
        return Flux.fromIterable(ids)
                .flatMap(this::findById);
    }

    public Mono<Integer> getStock(String productId) {
        return findById(productId)
                .map(Product::getStock);
    }

    private <T> Mono<T> maybeError(T value) {
        if (RANDOM.nextDouble() < 0.1) {
            return Mono.error(new RuntimeException("Erreur simulée de la base de données"));
        }
        return Mono.just(value);
    }
}


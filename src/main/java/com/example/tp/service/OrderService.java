package com.example.tp.service;

import com.example.tp.exception.InvalidOrderException;
import com.example.tp.model.*;
import com.example.tp.repository.ProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

public class OrderService {

    private final ProductRepository productRepository;

    public OrderService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Mono<Order> processOrder(OrderRequest request) {
        // 1. Valider la requête
        if (request == null || request.getProductIds() == null || request.getProductIds().isEmpty() || request.getCustomerId() == null) {
            return Mono.error(new InvalidOrderException("Requête invalide"));
        }

        // 2. Convertir en Flux, filtrer les IDs vides/null, limiter à 100
        return Flux.fromIterable(request.getProductIds())
                .filter(id -> id != null && !id.isEmpty())
                .take(100)
                // 3. Récupérer chaque produit
                .flatMap(id -> productRepository.findById(id)
                        .onErrorResume(e -> {
                            System.out.println("Erreur pour le produit " + id + ": " + e.getMessage());
                            return Mono.empty();
                        })
                )
                // Filtrer les produits en stock
                .filter(product -> product.getStock() > 0)
                .doOnNext(product -> System.out.println("Produit récupéré: " + product.getName()))
                // 4. Appliquer les réductions
                .map(product -> {
                    int discount = product.getCategory().equals("electronique") ? 10 : 5;
                    BigDecimal originalPrice = product.getPrice();
                    BigDecimal reduction = originalPrice.multiply(BigDecimal.valueOf(discount)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    BigDecimal finalPrice = originalPrice.subtract(reduction);
                    return new ProductWithPrice(product, originalPrice, discount, finalPrice);
                })
                // 5. Combiner les résultats
                .collectList()
                .map(productList -> {
                    BigDecimal totalPrice = productList.stream()
                            .map(ProductWithPrice::getFinalPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    boolean hasDiscount = productList.stream().anyMatch(p -> p.getDiscountPercentage() > 0);
                    return new Order(request.getProductIds(), productList, totalPrice, hasDiscount, OrderStatus.COMPLETED);
                })
                // 6. Gestion d'erreurs
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> System.out.println("Erreur lors du traitement: " + e.getMessage()))
                .onErrorResume(e -> {
                    Order failedOrder = new Order();
                    failedOrder.setProductIds(request.getProductIds());
                    failedOrder.setProducts(List.of());
                    failedOrder.setTotalPrice(BigDecimal.ZERO);
                    failedOrder.setDiscountApplied(false);
                    failedOrder.setStatus(OrderStatus.FAILED);
                    return Mono.just(failedOrder);
                })
                // 7. Logging
                .doOnNext(order -> System.out.println("Commande créée: " + order.getOrderId() + " - Status: " + order.getStatus()))
                .doFinally(signal -> System.out.println("Fin du traitement de la commande"));
    }
}


package com.example.tp1;

import com.example.tp1.exception.InvalidOrderException;
import com.example.tp1.model.*;
import com.example.tp1.repository.ProductRepository;
import com.example.tp1.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTest {

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ProductRepository repository = new ProductRepository();
        orderService = new OrderService(repository);
    }

    @Test
    void test_processOrderSuccess() {
        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD001", "PROD002"),
                "CUST001"
        );

        StepVerifier.create(orderService.processOrder(request))
                .assertNext(order -> {
                    assertThat(order.getOrderId()).isNotNull();
                    assertThat(order.getStatus()).isIn(OrderStatus.COMPLETED, OrderStatus.FAILED);
                    if (order.getStatus() == OrderStatus.COMPLETED) {
                        assertThat(order.getProducts()).isNotEmpty();
                        assertThat(order.getTotalPrice()).isGreaterThan(BigDecimal.ZERO);
                    }
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderWithInvalidIds() {
        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD001", "INVALID_ID", "PROD003", "UNKNOWN"),
                "CUST001"
        );

        StepVerifier.create(orderService.processOrder(request))
                .assertNext(order -> {
                    assertThat(order.getOrderId()).isNotNull();
                    assertThat(order.getStatus()).isIn(OrderStatus.COMPLETED, OrderStatus.FAILED);
                    if (order.getStatus() == OrderStatus.COMPLETED) {
                        // seuls les produits valides sont traités
                        assertThat(order.getProducts().size()).isLessThanOrEqualTo(2);
                    }
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderWithoutStock() {
        // PROD004 a un stock de 0
        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD004"),
                "CUST001"
        );

        StepVerifier.create(orderService.processOrder(request))
                .assertNext(order -> {
                    assertThat(order.getOrderId()).isNotNull();
                    if (order.getStatus() == OrderStatus.COMPLETED) {
                        assertThat(order.getProducts()).isEmpty();
                        assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
                    }
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderWithDiscounts() {
        // PROD001 = electronique, PROD003 = mobilier
        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD001", "PROD003"),
                "CUST001"
        );

        StepVerifier.create(orderService.processOrder(request))
                .assertNext(order -> {
                    assertThat(order.getOrderId()).isNotNull();
                    if (order.getStatus() == OrderStatus.COMPLETED && order.getProducts().size() == 2) {
                        for (ProductWithPrice pwp : order.getProducts()) {
                            if (pwp.getProduct().getCategory().equals("electronique")) {
                                assertThat(pwp.getDiscountPercentage()).isEqualTo(10);
                            } else {
                                assertThat(pwp.getDiscountPercentage()).isEqualTo(5);
                            }
                            BigDecimal expectedFinal = pwp.getOriginalPrice().subtract(
                                    pwp.getOriginalPrice().multiply(BigDecimal.valueOf(pwp.getDiscountPercentage()))
                                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                            );
                            assertThat(pwp.getFinalPrice()).isEqualByComparingTo(expectedFinal);
                        }
                        BigDecimal expectedTotal = order.getProducts().stream()
                                .map(ProductWithPrice::getFinalPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        assertThat(order.getTotalPrice()).isEqualByComparingTo(expectedTotal);
                    }
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderTimeout() {
        // Créer un repository avec un délai > 5s
        ProductRepository slowRepo = new ProductRepository() {
            @Override
            public Mono<Product> findById(String id) {
                return Mono.delay(Duration.ofSeconds(10))
                        .then(super.findById(id));
            }
        };
        OrderService slowService = new OrderService(slowRepo);

        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD001"),
                "CUST001"
        );

        StepVerifier.withVirtualTime(() -> slowService.processOrder(request))
                .thenAwait(Duration.ofSeconds(6))
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderWithErrors() {
        // Repository qui échoue 50% du temps
        ProductRepository errorRepo = new ProductRepository() {
            private final Random random = new Random();
            @Override
            public Mono<Product> findById(String id) {
                if (random.nextDouble() < 0.5) {
                    return Mono.error(new RuntimeException("Erreur simulée"));
                }
                return super.findById(id);
            }
        };
        OrderService errorService = new OrderService(errorRepo);

        OrderRequest request = new OrderRequest(
                Arrays.asList("PROD001", "PROD002", "PROD003", "PROD005"),
                "CUST001"
        );

        StepVerifier.create(errorService.processOrder(request))
                .assertNext(order -> {
                    assertThat(order.getOrderId()).isNotNull();
                    // la commande est créée même avec des erreurs partielles
                    assertThat(order.getStatus()).isIn(OrderStatus.COMPLETED, OrderStatus.FAILED);
                })
                .verifyComplete();
    }

    @Test
    void test_processOrderInvalidRequest() {
        OrderRequest request = new OrderRequest(null, null);

        StepVerifier.create(orderService.processOrder(request))
                .expectError(InvalidOrderException.class)
                .verify();
    }
}


# TP1 - Pipeline réactif de traitement de commandes

## Description
Système de traitement de commandes réactif pour une boutique en ligne, utilisant Spring WebFlux et Project Reactor.

## Architecture

### Modèles (`model/`)
- **Product** : produit avec id, nom, prix, stock et catégorie
- **ProductWithPrice** : produit avec prix original, réduction appliquée et prix final
- **Order** : commande avec liste de produits, prix total, statut
- **OrderRequest** : requête d'entrée avec liste d'IDs produits et ID client
- **OrderStatus** : enum (CREATED, VALIDATED, PROCESSING, COMPLETED, FAILED)

### Repository (`repository/`)
- **ProductRepository** : simule une base de données avec 5 produits en mémoire, délai de 100ms par appel, erreurs aléatoires à 10%, et cache simple pour éviter les requêtes redondantes

### Service (`service/`)
- **OrderService** : pipeline réactif `processOrder(OrderRequest) → Mono<Order>`

## Pipeline réactif (OrderService)
1. Validation de la requête (IDs non vides, customerId non null)
2. Conversion `List<String>` → `Flux<String>`, filtre des IDs vides/null, limite à 100
3. Récupération de chaque produit via `flatMap` → `findById()` avec `onErrorResume`
4. Filtre des produits hors stock (`stock > 0`)
5. Application des réductions : 10% électronique, 5% autres catégories
6. `collectList()` + calcul du prix total → création de l'Order (status COMPLETED)
7. `timeout(5s)` + `onErrorResume` → Order avec status FAILED en cas d'erreur
8. Logging : `doOnNext`, `doOnError`, `doFinally`

## Optimisation bonus : Caching (Option B)
Cache `ConcurrentHashMap<String, Product>` dans le `ProductRepository` pour stocker les produits déjà récupérés et éviter les requêtes redondantes.

## Tests (7 tests)
1. `test_processOrderSuccess` — cas nominal avec 2 produits valides
2. `test_processOrderWithInvalidIds` — mélange d'IDs valides et invalides
3. `test_processOrderWithoutStock` — produit avec stock = 0
4. `test_processOrderWithDiscounts` — vérification des réductions par catégorie
5. `test_processOrderTimeout` — timeout après 5s → status FAILED
6. `test_processOrderWithErrors` — erreurs partielles du repository
7. `test_processOrderInvalidRequest` — requête invalide → InvalidOrderException

## Lancer les tests
```bash
./mvnw test
```


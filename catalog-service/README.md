# catalog-service — Microservicio de Catálogo (Ecommerce)

Ejemplo didáctico de un microservicio Spring Boot (Java 17) con **Arquitectura Hexagonal + DDD**, orientado al mundo *read-heavy*: CRUD de productos, búsqueda paginada, categorías jerárquicas y estados de producto. Es la **fuente de verdad** del catálogo y **publica sus eventos a Kafka** para que el resto del ecosistema se entere sin llamarlo por HTTP.

## Quick path

```bash
./mvnw verify                 # unitarios + integración + ITs con Testcontainers
./mvnw test                   # solo unitarios + integración web (sin Docker)
./mvnw spring-boot:run        # levanta el servicio en http://localhost:8082
```

Probar el flujo de un producto:

```bash
# 1. Crear producto (nace en estado DRAFT)
curl -s -X POST http://localhost:8082/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name": "Notebook Pro", "description": "Laptop 16GB", "price": 1500.00, "currency": "USD"}'
# -> 201 Created, header Location: /api/products/<id>

# 2. Activarlo (publica el evento PRODUCT_ACTIVATED a Kafka)
curl -s -X POST http://localhost:8082/api/products/<id>/activate   # -> 204

# 3. Buscar con paginación
curl -s "http://localhost:8082/api/products?q=notebook&page=0&size=10"

# 4. Cambiar precio (publica PRODUCT_PRICE_CHANGED)
curl -s -X POST http://localhost:8082/api/products/<id>/price \
  -H 'Content-Type: application/json' -d '{"price": 1300.00, "currency": "USD"}'  # -> 204

# 5. Retirar (publica PRODUCT_RETIRED; orden-service dejará de venderlo)
curl -s -X POST http://localhost:8082/api/products/<id>/retire      # -> 204
```

## Stack

| Dependencia | Uso |
|---|---|
| Spring Boot 3.5.16 | Framework base (web, validation, actuator) |
| Java 17 | Records para value objects y DTOs, interfaz `sealed` para eventos |
| Spring Kafka | Producer del topic `catalog.products` (JsonSerializer, **sin type headers**) |
| Testcontainers + awaitility | ITs con Kafka real en contenedor (`*IT`, corre con `mvn verify`) |
| JUnit 5 + Mockito + AssertJ | Tests unitarios y de integración |
| JaCoCo | Reporte de cobertura (`target/site/jacoco/index.html`) |

## Contrato del topic `catalog.products`

Los eventos se serializan como JSON plano (**sin `__TypeId__`** de Jackson): cada
micro define su propia clase del contrato, así no comparten código.

- **Key** del mensaje: `companyId:productId` → misma key, misma partición, orden
  garantizado por tenant.
- **Fechas ISO-8601**: el producer usa `IsoDateJsonSerializer` (mapper con
  `WRITE_DATES_AS_TIMESTAMPS` desactivado). El `JsonSerializer` default de Spring
  Boot serializa `Instant` como epoch en notación científica — eso rompía el
  contrato. `CatalogProductContractIT` fija ambas cosas en piedra.

El payload:

```json
{
  "eventType": "PRODUCT_CREATED | PRODUCT_UPDATED | PRODUCT_PRICE_CHANGED | PRODUCT_ACTIVATED | PRODUCT_RETIRED",
  "productId": "uuid",
  "productName": "Notebook Pro",
  "price": 1300.00,
  "currency": "USD",
  "status": "DRAFT | ACTIVE | RETIRED",
  "occurredAt": "2026-08-10T12:00:00Z",
  "companyId": "uuid"
}
```

Consumidor (order-service) en `infrastructure/adapter/in/kafka/CatalogProductEvent`.

## Arquitectura hexagonal en 30 segundos

```
                     ┌─────────────────────────────────────────┐
                     │              INFRAESTRUCTURE             │
                     │  (adapters: lo que cambia)               │
   HTTP ───────────► │  web/ProductController, CategoryController │
                     │  out/persistence/InMemory    ← puerto OUT │
                     │  out/kafka/KafkaEventPublisher → Kafka    │
                     └──────────────┬───────────────────────────┘
                                    │ depende de interfaces (puertos)
                     ┌──────────────▼───────────────────────────┐
                     │              APPLICATION                  │
                     │  CatalogApplicationService               │
                     │  (11 casos de uso ISP: create, update,   │
                     │   search, activate, retire, categories…) │
                     └──────────────┬───────────────────────────┘
                                    │
                     ┌──────────────▼───────────────────────────┐
                     │                DOMAIN                     │
                     │  Product (aggregate), Category, Money,   │
                     │  ProductStatus, eventos sealed,          │
                     │  repositorios (puertos)                  │
                     │  (puro, sin frameworks)                  │
                     └──────────────────────────────────────────┘
```

**Regla de oro:** las flechas apuntan hacia adentro. El dominio decide *cuándo* se
publica un evento (`pullDomainEvents()`); la infraestructura decide *cómo* (Kafka).
Si mañana cambiás Kafka por RabbitMQ o por un bus en memoria, el dominio y los casos
de uso no se tocan.

## Los building blocks de DDD usados

| Concepto | Implementación |
|---|---|
| **Aggregate root** | `Product` — valida sus invariantes y registra eventos de dominio |
| **Value objects** | `ProductId`, `CategoryId`, `Money` (records inmutables con validación) |
| **Máquina de estados** | `ProductStatus` valida transiciones: `DRAFT → ACTIVE → RETIRED` (y `RETIRED` terminal) |
| **Eventos de dominio** | `ProductCreated`, `ProductUpdated`, `ProductPriceChanged`, `ProductActivated`, `ProductRetired` |
| **Categorías jerárquicas** | `Category` con `parentId`, sin ciclos (validado por el dominio) |
| **Tenancy** | `CompanyId` (value object) en `Product` y `Category`; repositorios escopados por company; el tenant entra por el seam `CompanyContext` en el adapter web |

## Tests

| Suite | Qué cubre |
|---|---|
| `domain/model/*Test` | Invariantes de `Product`, `Category`, aritmética de `Money`, tabla de transiciones |
| `application/service/CatalogApplicationServiceTest` | Los 11 casos de uso con repositorios en memoria |
| `infrastructure/adapter/in/web/*ControllerTest` | Slice web: 201/400/404, validación y search paginado |
| `CatalogServiceIntegrationTest` | End-to-end por la API con el publisher mockeado |
| `KafkaEventPublisherIT` | **Con Kafka real (Testcontainers)**: publica y verifica el evento con nombre/precio/status correctos |
| `KafkaPartitioningIT` | **Con Kafka real**: verifica el particionado por key (`companyId:productId`) |
| `CatalogProductContractIT` | **Prueba de contrato (consumer-driven)**: el JSON publicado tiene los campos que order espera, fechas ISO-8601 y key `companyId:productId` |

## Cómo evolucionar el ejemplo

- **Conectar Postgres**: creá `JpaProductRepository` / `JpaCategoryRepository` y cambiá los adapters. Nada más.
- **Agregar búsqueda con Elasticsearch**: implementá el puerto de búsqueda con otro adapter.
- **Publicar a otro broker**: cambiá `KafkaEventPublisher` por otro adapter; el caso de uso no se entera.

## Contexto del ecosistema

Este es uno de los micros del ecommerce. Construidos: `catalog-service` (este
micro), `order-service` (consume `catalog.products` para mantener su snapshot y
resolver precios al crear órdenes) y `warehouse-service` (esqueleto, segundo
consumidor). El endgame es **cerrar en estos 3 micros** y luego la capa
transversal: patrón **Outbox** (hoy se publica directo a Kafka, sin transacción),
**idempotencia** del consumidor, **Saga** para el flujo de compra y API Gateway.
Ver [../PLAN.md](../PLAN.md).

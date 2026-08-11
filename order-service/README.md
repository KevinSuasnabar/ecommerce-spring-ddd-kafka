# order-service — Microservicio de Órdenes (Ecommerce)

Ejemplo didáctico de un microservicio Spring Boot (Java 17) con **Arquitectura Hexagonal + DDD**, pensado para enseñar el patrón: el **dominio** es el corazón de la aplicación y **no depende de nada** — ni de Spring, ni de HTTP, ni de la base de datos.

## Quick path

```bash
./mvnw verify                 # unitarios + integración + ITs con Testcontainers
./mvnw test                   # solo unitarios + integración web (sin Docker)
./mvnw spring-boot:run        # levanta el servicio en http://localhost:8081
```

> Requiere el broker Kafka corriendo (ver el `docker-compose.yml` de la raíz) y
> que `catalog-service` esté publicando productos al topic `catalog.products`.

Probar el flujo completo de una orden:

```bash
# 1. Crear orden. Ojo: SOLO mandás productId y quantity.
#    El nombre y el precio NO los manda el cliente: se resuelven desde el
#    snapshot que este micro mantiene escuchando los eventos de catalog-service.
curl -s -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "90000000-0000-0000-0000-000000000001",
    "lines": [{"productId": "10000000-0000-0000-0000-000000000001",
               "quantity": 1}],
    "shippingAddress": {"street": "Av. Siempre Viva 123", "city": "Springfield",
                        "state": "", "country": "AR", "zipCode": "1406"},
    "paymentMethod": "CREDIT_CARD"
  }'
# -> 201 Created, header Location: /api/orders/<id>
# -> 404 PRODUCT_NOT_AVAILABLE si el producto no está ACTIVO en el snapshot

# 2. Confirmar (paga y reserva stock) -> 204
curl -s -X POST http://localhost:8081/api/orders/<id>/confirm

# 3. Consultar -> status CONFIRMED
curl -s http://localhost:8081/api/orders/<id>
```

## Integración event-driven con catalog-service

Este micro **no consulta** a catalog-service por HTTP. Escucha el topic
`catalog.products` y mantiene un **read model** (snapshot) de los productos:

```
catalog-service ──publica──► Kafka (catalog.products) ──consume──► order-service
                                ProductCreatedEvent                     │
                                ProductPriceChangedEvent                ▼
                                ProductActivatedEvent        CatalogEventConsumer
                                ProductRetiredEvent                │
                                                                    ▼
                                                    InMemoryCatalogProductStore
                                                    (snapshot: id, nombre, precio, status)
```

Cuando creás una orden, `OrderApplicationService` resuelve nombre/precio desde ese
snapshot y solo acepta productos con estado `ACTIVE` (si no, `PRODUCT_NOT_AVAILABLE`).
El precio queda **congelado en la línea de la orden** al momento de comprar — así un
cambio de precio posterior no afecta órdenes ya creadas. Todo es *eventual consistency*:
los eventos viajan asíncronos y el snapshot se actualiza en cuanto llegan.

## Stack

| Dependencia | Uso |
|---|---|
| Spring Boot 3.5.16 | Framework base (web, validation, actuator) |
| Java 17 | Records para value objects y DTOs, interfaces `sealed` para eventos |
| Spring Kafka | Consumer del topic `catalog.products` (eventos de catalog-service) |
| Testcontainers + awaitility | ITs con Kafka real en contenedor (`*IT`, corre con `mvn verify`) |
| JUnit 5 + Mockito + AssertJ | Tests unitarios y de integración |
| JaCoCo | Reporte de cobertura (`target/site/jacoco/index.html`) |

## Arquitectura hexagonal en 30 segundos

```
                     ┌─────────────────────────────────────────┐
                     │              INFRAESTRUCTURE             │
                     │  (adapters: lo que cambia)               │
   HTTP ───────────► │  web/OrderController  ← puerto IN        │
                     │  persistence/InMemory  ← puerto OUT      │
                     │  payment/Fake, inventory/Fake,           │
                     │  notification/Log, event/Spring          │
                     └──────────────┬───────────────────────────┘
                                    │ depende de interfaces (puertos)
                     ┌──────────────▼───────────────────────────┐
                     │              APPLICATION                  │
                     │  OrderApplicationService                 │
                     │  (casos de uso: orquesta los puertos)    │
                     └──────────────┬───────────────────────────┘
                                    │
                     ┌──────────────▼───────────────────────────┐
                     │                DOMAIN                     │
                     │  Order (aggregate), Money, Address,      │
                     │  OrderStatus, eventos, repositorio       │
                     │  (puro, sin frameworks)                  │
                     └──────────────────────────────────────────┘
```

**Regla de oro:** las flechas apuntan hacia adentro. `domain` no importa nada de `application` ni de `infrastructure`. Si mañana cambiás la BD simulada por Postgres, o el pago fake por Stripe, **el dominio y los casos de uso no se tocan**.

## Estructura del proyecto

```
src/main/java/com/ecommerce/order
├── domain/                      # NÚCLEO. Sin dependencias externas.
│   ├── model/                   # Agregado Order + value objects
│   ├── event/                   # Eventos de dominio (sealed interface)
│   ├── repository/              # Puerto OrderRepository
│   └── exception/               # Excepciones de negocio
├── application/                 # Casos de uso (orquestación)
│   ├── port/in/                 # Puertos de entrada (interfaces de casos de uso)
│   ├── port/out/                # Puertos de salida (Payment, Inventory, CatalogProductStore…)
│   ├── dto/                     # Comandos y resultados de consulta (incluye CatalogProduct)
│   └── service/                 # OrderApplicationService
└── infrastructure/              # Adapters. TODO lo que es Spring/HTTP/Kafka/BD.
    ├── adapter/in/              # web/ (controllers) y kafka/ (CatalogEventConsumer)
    └── adapter/out/             # persistence, payment, inventory, notification, event, catalog
```

## Los building blocks de DDD usados

| Concepto | Implementación |
|---|---|
| **Aggregate root** | `Order` — única puerta de entrada a sus invariantes; expone `pullDomainEvents()` |
| **Value objects** | `Money`, `Address`, `OrderId`, `CustomerId`, `ProductId`, `OrderLine` (records inmutables con validación en el constructor) |
| **Invariantes** | Una orden sin líneas no existe; cantidad > 0; `Money` no negativo; monedas homogéneas al sumar |
| **Eventos de dominio** | `OrderCreated`, `OrderConfirmed`, `OrderShipped`, `OrderDelivered`, `OrderCancelled` |
| **Máquina de estados** | `OrderStatus` valida las transiciones: `CREATED → CONFIRMED → SHIPPED → DELIVERED`, con `CANCELLED` desde `CREATED`/`CONFIRMED` |

## SOLID en el código (no en teoría)

| Principio | Dónde lo ves |
|---|---|
| **S** — Responsabilidad única | Un `value object` valida solo su propia regla; un caso de uso hace una sola cosa; el controller solo traduce HTTP ↔ comandos |
| **O** — Abierto/cerrado | Agregás un método de pago nuevo creando una clase `PaymentStrategy`; el core no se modifica |
| **L** — Sustitución de Liskov | Los fakes de `PaymentPort`/`InventoryPort` son intercambiables por los reales sin tocar `OrderApplicationService` |
| **I** — Segregación de interfaces | `CreateOrderUseCase`, `ConfirmOrderUseCase`, `GetOrderUseCase`… son interfaces separadas (nada de un servicio gordo) |
| **D** — Inversión de dependencias | `application` y `domain` dependen de puertos (interfaces), nunca de clases concretas de infraestructura |

## Patrones de diseño aplicados

| Patrón | Dónde |
|---|---|
| **Ports & Adapters** | La base de la arquitectura hexagonal |
| **Repository** | `OrderRepository` abstrae la persistencia |
| **Domain Event** | El agregado registra eventos; `EventPublisher` los propaga |
| **Strategy + Factory** | `PaymentStrategy` + `PaymentStrategyFactory` elige el método de pago por `enum` |
| **Adapter** | `FakePaymentAdapter`, `FakeInventoryAdapter`, `LogNotificationAdapter` |
| **Factory method** | `Order.create(...)` encapsula la construcción del agregado |
| **Command** | Cada caso de uso recibe un comando inmutable (`CreateOrderCommand`) |

## BD simulada

`InMemoryOrderRepository` (un `ConcurrentHashMap`) cumple el contrato de `OrderRepository` con el mismo API que tendría una persistencia real. El stock vive en `FakeInventoryAdapter`, que arranca con:

- `10000000-0000-0000-0000-000000000001` → Notebook (100 uds)
- `10000000-0000-0000-0000-000000000002` → Mouse (50 uds)

Cualquier otro `productId` responde "sin stock" → la confirmación devuelve **422**.

El snapshot del catálogo vive en `InMemoryCatalogProductStore`, poblado por el
`CatalogEventConsumer` cuando llegan eventos de catalog-service.

## Tests

| Suite | Qué cubre |
|---|---|
| `domain/model/*Test` | Invariantes del agregado, aritmética de `Money`, tabla de transiciones |
| `application/service/OrderApplicationServiceTest` | Casos de uso con puertos mockeados: creación (resolviendo nombre/precio del snapshot), pagos rechazados, stock insuficiente, transiciones inválidas, producto no disponible |
| `infrastructure/adapter/in/web/OrderControllerTest` | Slice web con `@WebMvcTest`: 201/400/404/204 y validación |
| `OrderServiceIntegrationTest` | End-to-end con el contexto completo: ciclo de vida por la API, stock agotado (422) y producto no disponible (404) |
| `CatalogEventConsumerIT` | **Con Kafka real (Testcontainers)**: consume eventos y actualiza el snapshot (requiere Docker, corre con `mvn verify`) |

Cobertura: `./mvnw verify` genera el reporte en `target/site/jacoco/index.html`. El dominio (la parte que más te conviene proteger) tiene cobertura casi total.

## Cómo evolucionar el ejemplo

- **Conectar Postgres**: creá `JpaOrderRepository implements OrderRepository` y cambiá el adapter. Nada más.
- **Agregar Stripe**: implementá `PaymentPort` con el SDK real, o agregá una `PaymentStrategy` nueva.
- **Persistir el snapshot**: `CatalogProductStore` ya es un puerto; cambiá `InMemoryCatalogProductStore` por un adapter con BD.
- **Manejar mensajes "venenos"**: en producción conviene envolver el deserializador con `ErrorHandlingDeserializer` + un recoverer para no frenar el consumo ante un evento corrupto.

## Contexto del ecosistema

Este es uno de los micros del ecommerce. Construidos: `order-service` (este
micro), `catalog-service` (fuente de verdad de productos, publica sus eventos a
Kafka) y `warehouse-service` (esqueleto, segundo consumidor). El endgame es
**cerrar en estos 3 micros** y luego la capa transversal: patrón **Outbox**,
**idempotencia** del consumidor, **Saga** para el flujo de compra y API Gateway.
Ver [../PLAN.md](../PLAN.md).

> **Próximo paso de este micro**: tenancy. El evento ya trae `companyId` (y la key
> del topic es `companyId:productId`), pero el snapshot y las órdenes todavía son
> ciegos al tenant. Falta aplicar el patrón `CompanyContext` que catalog ya usa.

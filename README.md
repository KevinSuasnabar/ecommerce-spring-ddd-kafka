# hexagonal-micro — Ecosistema Ecommerce en Microservicios (Hexagonal + DDD)

Proyecto didáctico: un ecommerce construido microservicio por microservicio con
**Arquitectura Hexagonal (Ports & Adapters) + DDD**, comunicados por **eventos**
via **Kafka**. El objetivo es aprender arquitectura de software con código real,
entendiendo el *porqué* de cada decisión antes que el *cómo*.

## Microservicios

| Micro | Puerto | Rol | Estado |
|---|---|---|---|
| `catalog-service` | 8082 | Fuente de verdad del catálogo. CRUD de productos, search paginado, categorías jerárquicas, máquina de estados DRAFT/ACTIVE/RETIRED. **Publica** eventos al topic `catalog.products` | ✅ |
| `order-service` | 8081 | Órdenes con ciclo de vida (created → confirmed → shipped → delivered). **Consume** `catalog.products` y mantiene un snapshot para resolver nombre/precio sin llamar a catalog por HTTP | ✅ |
| `cart`, `inventory`, `payment`, `customer`, `shipment`, `notification` | — | Pendientes (plan) | ⏳ |

## Comunicación entre micros (event-driven)

```
catalog-service ──► Kafka (topic catalog.products) ──► order-service
   create/update       { eventType, productId,             CatalogEventConsumer
   price/activate/        productName, price,                  │
   retire                 currency, status, occurredAt }      ▼
                                                    snapshot local (read model)
```

- **Sin llamadas HTTP síncronas** entre micros: el contrato es el **evento del topic**.
- Cada micro define su propia clase del contrato (**sin type headers de Jackson**),
  así no comparten código ni se acoplan.
- La consistencia es **eventual**: el snapshot de order se actualiza cuando el evento llega.
- Las órdenes **congelan el precio** en el momento de la compra.

## Stack compartido

- **Spring Boot 3.5.16** / Java 17 (records, `sealed` interfaces)
- **Kafka** vía `apache/kafka:3.7.0` (KRaft) en Docker Compose
- **Tests**: JUnit 5, Mockito, AssertJ, Testcontainers (Kafka real en los ITs), awaitility, JaCoCo

## Arranque rápido

```bash
# 1. Broker Kafka (contenedor ecommerce-kafka en localhost:9092)
docker compose up -d

# 2. Levantar ambos micros (dos terminales)
(cd catalog-service && ./mvnw spring-boot:run)
(cd order-service && ./mvnw spring-boot:run)

# 3. Correr los tests (unitarios + integración web)
(cd catalog-service && ./mvnw verify)
(cd order-service && ./mvnw verify)
```

> `./mvnw verify` corre también los `*IT` con Testcontainers (requieren Docker).
> `./mvnw test` corre todo lo demás sin Docker.

## Demo end-to-end

```bash
PID=$(curl -s -X POST http://localhost:8082/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Teclado Mecánico","price":75.00,"currency":"USD"}' \
  | sed -n 's/.*"productId":"\([0-9a-f-]*\)".*/\1/p')

curl -s -X POST http://localhost:8082/api/products/$PID/activate   # 204

curl -s -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d "{\"customerId\":\"90000000-0000-0000-0000-000000000001\",
       \"lines\":[{\"productId\":\"$PID\",\"quantity\":2}],
       \"shippingAddress\":{\"street\":\"Av. Siempre Viva 123\",\"city\":\"Springfield\",
                            \"state\":\"\",\"country\":\"AR\",\"zipCode\":\"1406\"},
       \"paymentMethod\":\"CREDIT_CARD\"}"
# -> 201. El nombre y el precio salen del snapshot de order-service, NO del cliente.
```

## Estructura del repo

```
hexagonal-micro/
├── docker-compose.yml      # Kafka KRaft (single node, puerto 9092, auto-create topics)
├── catalog-service/        # README propio, puro productor de eventos
└── order-service/          # README propio, consumidor + snapshot read model
```

## Siguiente micro del plan

`cart` o `inventory` (candidatos), luego la capa transversal (API Gateway, Service
Discovery, Config Server) y el patrón **Saga** para el flujo de compra completo.

## Detalle de cada micro

La arquitectura hexagonal, los building blocks de DDD, SOLID y los patrones aplicados
se explican en los README de cada servicio:
- [order-service/README.md](order-service/README.md)
- [catalog-service/README.md](catalog-service/README.md)

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
| `warehouse-service` | 8083 | Stock. Segundo consumidor de `catalog.products` (read model de inventario) | 🚧 esqueleto |

> **Endgame**: cerrar en estos 3 micros + capa transversal (Outbox, idempotencia,
> Saga, API Gateway). Ver [PLAN.md](PLAN.md) — no se agregan más micros.

## Comunicación entre micros (event-driven)

```
catalog-service ──► Kafka (topic catalog.products) ──► order-service
   create/update       key: companyId:productId          CatalogEventConsumer
   price/activate/     { eventType, productId,              │
   retire                productName, price, currency,      ▼
                         status, occurredAt(ISO-8601),   snapshot local (read model)
                         companyId }
```

- **Sin llamadas HTTP síncronas** entre micros: el contrato es el **evento del topic**.
- Cada micro define su propia clase del contrato (**sin type headers de Jackson**),
  así no comparten código ni se acoplan.
- **Key `companyId:productId`** → todos los eventos de un tenant caen en la misma
  partición **en orden** (contrato fijado por `CatalogProductContractIT`).
- **Fechas ISO-8601** en el wire (el producer usa `IsoDateJsonSerializer`).
- La consistencia es **eventual**: el snapshot de order se actualiza cuando el evento llega.
- Las órdenes **congelan el precio** en el momento de la compra.

## Multi-tenancy

Tenancy **row-level** por `CompanyId` (value object del dominio). El tenant se
resuelve en el borde HTTP vía el seam `CompanyContext` (adapter web) y se propaga
como argumento explícito hasta el dominio y los repositorios escopados.

| Micro | Estado |
|---|---|
| `catalog-service` | ✅ productos y categorías escopados por company |
| `order-service` | ⏳ pendiente (el evento ya trae `companyId`; falta aplicarlo) |
| `warehouse-service` | ⏳ se construye ya con tenancy |

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
├── PLAN.md                   # roadmap vigente, estado, contratos y deuda
├── AGENTS.md                 # convenciones e invariants para agentes de IA
├── docker-compose.yml        # Kafka KRaft (single node, puerto 9092, auto-create topics)
├── scripts/create-micro.sh   # scaffolder de micros hexagonal (plantillas en scripts/templates)
├── catalog-service/          # README propio, puro productor de eventos
├── order-service/            # README propio, consumidor + snapshot read model
└── warehouse-service/        # esqueleto, segundo consumidor (stock)
```

## Roadmap

El plan completo (estado, contratos, deuda técnica y próximos pasos) vive en
**[PLAN.md](PLAN.md)**. Resumen: tenancy en order → warehouse desde cero → capa
transversal (Outbox, idempotencia, Saga, Gateway) → CI.

## Detalle de cada micro

La arquitectura hexagonal, los building blocks de DDD, SOLID y los patrones aplicados
se explican en los README de cada servicio:
- [order-service/README.md](order-service/README.md)
- [catalog-service/README.md](catalog-service/README.md)

# PLAN — hexagonal-micro

> **Proyecto de aprendizaje.** El objetivo no es construir un ecommerce completo,
> sino aprender arquitectura de software (Hexagonal + DDD + event-driven) con
> código real, entendiendo el *porqué* de cada decisión. Cuando un paso ya no
> enseña nada nuevo, se descarta.

## Endgame acordado

**Cerrar en 3 microservicios** (`catalog`, `order`, `warehouse`) y después invertir
el esfuerzo en la **capa transversal**, que es donde está el aprendizaje que falta.

Se **descartan** como micros: `cart`, `payment`, `customer`, `shipment`,
`notification`, `inventory`. Razón: cada uno agregaría infraestructura repetitiva
(sin concepto arquitectónico nuevo) y el valor de aprendizaje decrece. El único
concepto que aportaría un micro extra (sagas de pago/compensación) se cubre mejor
en la capa transversal con los 3 micros existentes.

| Micro | Concepto que enseña |
|---|---|
| `catalog-service` | Hexagonal, DDD, máquina de estados, **tenancy** |
| `order-service` | Event-driven, snapshot/read model, consistencia eventual |
| `warehouse-service` | Construir un micro **desde cero** (scaffolder), segundo consumidor |
| **capa transversal** | **Outbox, idempotencia, Saga, API Gateway** — el aprendizaje pendiente |

## Estado actual (verificado contra el repo)

| Micro | Estado | Tests |
|---|---|---|
| `catalog-service` | Completo + **tenancy por `CompanyId`** | 56 unit + 3 ITs Kafka (incl. prueba de contrato) |
| `order-service` | Completo v1 + **tenancy por `CompanyId`** (snapshot y órdenes escopados por tenant) | 61 unit + 5 ITs Kafka |
| `warehouse-service` | **Esqueleto**: solo `Application` + yml (ya configurado como consumidor de `catalog.products`) | 0 |

**Total: 125 tests en verde** (catalog 59, order 66). `./mvnw verify` completo del monorepo pasa.

## Contratos vigentes (no romper sin actualizar esto)

- **Topic** `catalog.products`. **Key** = `companyId:productId` (garantiza orden por
  tenant: misma key → misma partición). Fijado por `CatalogProductContractIT`.
- **Payload** JSON plano, **sin type headers** de Jackson (`spring.json.add.type.headers: false`).
  Cada micro define su propia clase del contrato; no comparten código.
- **Fechas ISO-8601** (`occurredAt`). El producer usa `IsoDateJsonSerializer`
  (mapper con `WRITE_DATES_AS_TIMESTAMPS` desactivado) — el default de Spring Boot
  serializa `Instant` como epoch en notación científica y rompía el contrato.
- Campos del payload: `eventType, productId, productName, price, currency, status,
  occurredAt, companyId`. Los consumidores toleran campos extra que no conocen
  (order ya consume `companyId` para escopar su snapshot; la key sigue siendo la
  fuente de verdad del tenant).

## Roadmap

### Paso 1 — Tenancy en `order-service` (TDD)  ✅ completado
El evento YA trae `companyId` (la key lo garantiza), pero order lo ignoraba: el
snapshot y las órdenes eran ciegos al tenant. Se transfirió el patrón `CompanyContext`
(seam ya probado en catalog) a un segundo micro.
- `CompanyId` value object en el dominio de order (misma forma que catalog)
- Snapshot (`CatalogProductStore`) escopado por company (composite key igual que catalog)
- `Order` ganó `CompanyId`; `OrderRepository` escopado; los eventos de dominio lo llevan
- `CompanyContext` en el adapter web (mismo seam que catalog, mismo UUID hardcodeado)

### Paso 2 — `warehouse-service` desde cero  ⏳ siguiente
Primer micro **construido por el estudiante** usando `scripts/create-micro.sh`.
Dominio de stock + consumidor de `catalog.products` (segundo read model). Yo guío,
el estudiante escribe.

### Paso 3 — Capa transversal (el aprendizaje fuerte)
- **Patrón Outbox**: hoy `KafkaEventPublisher` publica directo; si falla después del
  `save`, se pierde el evento. Es un bug arquitectónico real pendiente.
- **Idempotencia del consumidor** (dedupe por `eventId`)
- **Patrón Saga** para el flujo de compra completo
- **API Gateway** (y recién ahí tiene sentido hablar de Service Discovery/Config)

### Paso 4 — Calidad de proceso
- CI (GitHub Actions) que corra `./mvnw verify` — hoy la única puerta es manual
- JaCoCo con umbral mínimo de cobertura en el dominio

## Deuda técnica conocida

- `warehouse-service` no tiene tests ni dominio (esqueleto)
- `KafkaEventPublisher` publica sin Outbox (riesgo de pérdida de evento)
- Consumidores sin manejo de "mensajes venenosos" (`ErrorHandlingDeserializer`)
- Persistencia en memoria (`InMemory*`) — intencional para el ejemplo

## Comandos de verificación

```bash
./mvnw verify                 # monorepo completo: unitarios + ITs (requiere Docker)
./mvnw verify -pl <micro>     # un solo micro
./mvnw test                   # sin Docker (no corre los *IT)
```

## Convenciones

- **Idioma**: español rioplatense. Respuestas breves, pedagogía con el *porqué*.
- **Commits**: convencionales (`feat:`, `fix:`, `test:`, `docs:`), sin atribución IA.
- **Arquitectura**: las flechas apuntan hacia adentro. El dominio no conoce Spring,
  HTTP ni Kafka. Los eventos de dominio se publican vía `pullDomainEvents()`.

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
| `catalog-service` | Completo + **tenancy por `CompanyId`** + **Outbox** (sync send con `acks=all` + poller) | 56 unit + 3 ITs Kafka (incl. prueba de contrato) |
| `order-service` | Completo v1 + **tenancy por `CompanyId`** (snapshot y órdenes escopados por tenant) + **idempotencia** (dedupe por `eventId`) | 69 unit + 6 ITs Kafka |
| `warehouse-service` | Completo v1: dominio de stock con ledger (Variante B) + `stock_level` (saldo O(1)) + write-path O(1) (`Stock.fromSnapshot`) + consumidor de `catalog.products` + idempotencia + tenancy por `CompanyId` | 53 unit + 3 ITs Kafka |

**Total: 190 tests en verde** (catalog 59, order 75, warehouse 56). `./mvnw verify` completo del monorepo pasa.

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

### Paso 2 — `warehouse-service` desde cero  ✅ completado
Primer micro **construido desde cero**: dominio de stock con patrón ledger (Variante B),
consumidor de `catalog.products` (segundo read model), y tenancy replicada al tercer micro.
- Agregado `Stock` con ledger de `StockMovement` (RECEIVED, RESERVED, RELEASED)
- Cantidades `available` y `reserved` derivadas (no guardadas), invariantes de negocio
- `StockRepository` con composite key `StockId(CompanyId, ProductId)` — tenancy implícita
- `CompanyContext` en el adapter web (mismo seam que catalog y order)
- Consumer Kafka que sincroniza líneas de stock desde eventos del catálogo

### Paso 3 — Capa transversal (el aprendizaje fuerte)  ⏳ en curso

- ✅ **Patrón Outbox** — tabla `outbox_event` + poller con `send().get()` síncrono (`acks=all`). El evento se persiste en la misma TX que el `save` del producto y se publica desde un thread de background. Garantía at-least-once + idempotencia en consumidores = exactly-once efectivo.
- ✅ **Idempotencia del consumidor** — dedupe por `eventId` con `ProcessedEventStore` en order y warehouse.
- ⏳ **Patrón Saga** para el flujo de compra completo — **siguiente**
- ⏳ **API Gateway** (y recién ahí tiene sentido hablar de Service Discovery/Config)

### Paso 4 — Calidad de proceso
- CI (GitHub Actions) que corra `./mvnw verify` — hoy la única puerta es manual
- JaCoCo con umbral mínimo de cobertura en el dominio

## Deuda técnica conocida

- **Eliminar `InMemory*`** — las implementaciones en memoria ya no aportan aprendizaje; postgres es el camino. Implica tocar los 3 micros, todos los tests, y posiblemente usar Testcontainers o H2 para los tests que hoy dependen del perfil default.
- En el perfil default (`!postgres`), `catalog-service` publica eventos sin Outbox (`KafkaEventPublisher` directo).
- Consumidores sin manejo de "mensajes venenosos" (`ErrorHandlingDeserializer`).

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

## Próxima sesión (por donde arrancar)

1. **¿Eliminar `InMemory*`?** — toca los 3 micros y todos los tests. El usuario quiere que postgres sea el único camino.
2. **Saga** — patrón Saga para el flujo de compra (el concepto más fuerte que falta).
3. **API Gateway** — routing, rate limiting, etc.
4. **CI (GitHub Actions)** + umbral JaCoCo.

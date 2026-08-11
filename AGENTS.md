# AGENTS.md

Proyecto de **aprendizaje**: ecommerce event-driven con Hexagonal + DDD (Spring Boot
3.5.16 / Java 17 / Kafka). Monorepo: `catalog-service`, `order-service`, `warehouse-service`.

## Antes de trabajar

1. Leé **`PLAN.md`** — roadmap vigente, estado de cada micro y contratos.
2. El **siguiente paso del roadmap** está marcado ahí; no inventes otro alcance.

## Invariants (no violar)

- Las flechas apuntan hacia adentro: `domain` no importa Spring, HTTP ni Kafka.
- Entre micros **solo eventos** (topic `catalog.products`), nunca HTTP síncrono.
- Key del topic: `companyId:productId`. Fechas del payload: **ISO-8601**. Sin type headers.
- Tenancy row-level por `CompanyId` (seam `CompanyContext` en el adapter web).
- No agregues micros fuera del endgame de `PLAN.md` (3 micros + capa transversal).

## Verificación

```bash
./mvnw verify -pl <micro>   # unitarios + ITs (los *IT requieren Docker)
./mvnw test                 # sin Docker
```

Todo cambio debe dejar `./mvnw verify` en verde antes de considerarse hecho.

## Convenciones

- Español rioplatense; respuestas breves; explicar el *porqué* (es un proyecto de aprendizaje).
- Commits convencionales (`feat:`, `fix:`, `test:`, `docs:`), sin atribución IA.
- Tests: unitarios con Mockito estricto, slice web con `@WebMvcTest` + `@MockitoBean`,
  ITs con Testcontainers + Awaitility. Nada de tests que no fallen por la razón correcta.

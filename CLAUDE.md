# CLAUDE.md — api-contabilidad (Serviplus SA)

## Project

Accounting microservice for Serviplus SA. Manages cotizaciones (quotes), facturas (invoices),
and abonos (partial payments). Backend is Spring Boot; frontend is React (already complete).

## Quick commands

```bash
# Run everything
docker compose up --build

# ── Backend ──────────────────────────────────────────────────────────
# Unit tests only (Mockito, no DB — fast)
cd backend && ./mvnw test
# Integration tests + unit + JaCoCo coverage gate (Testcontainers starts MariaDB)
cd backend && ./mvnw verify
# Single test class
cd backend && ./mvnw test -Dtest=CotizacionServiceTest
# Single integration test
cd backend && ./mvnw verify -Dit.test=CotizacionIT

# ── Frontend ─────────────────────────────────────────────────────────
# Install deps (REQUIRED after package.json changes; commits package-lock.json)
cd frontend && npm install
# Dev server
cd frontend && npm run dev
# TypeScript strict type-check (no emit)
cd frontend && npm run type-check
# ESLint
cd frontend && npm run lint
# Prettier — format files
cd frontend && npm run format
# Prettier — check only (what CI runs)
cd frontend && npm run format:check
# Vitest unit tests
cd frontend && npm test
# Vitest with coverage report
cd frontend && npm run test:coverage
# Playwright E2E (API mocked — no backend needed)
cd frontend && npm run test:e2e
# Playwright with visible browser for debugging
cd frontend && npx playwright test --headed
```

## Frontend — important after dependency changes

After editing `package.json`, always run `npm install` and commit the updated
`package-lock.json`. CI uses `npm ci` which requires a locked file in sync.

## Tech stack — EXACT versions (never guess or upgrade without asking)

| Component   | Version            | Notes                                   |
| ----------- | ------------------ | --------------------------------------- |
| Java        | 25                 | pom.xml java.version=25                 |
| Spring Boot | 4.0.6              | Parent POM                              |
| MariaDB     | 11.4-jammy         | Docker image                            |
| Redis       | 7.4-alpine         | Docker image; @Async in-process for now |
| MinIO       | 2024-12-18 release | PDF storage                             |
| Kong        | 3.8                | DB-less; kong.yml in ./kong/            |
| React       | 18.3.1             | Frontend (DO NOT MODIFY unless asked)   |
| Node.js     | 20.18 LTS          | Frontend builds                         |
| Prettier    | 3.4.2              | Frontend formatter                      |
| Testcontainers | 1.21.1          | Integration tests (BOM explicit in pom) |

## Architecture — 7 strict layers (ADR-M1, ADR-M3)

```
View → Serializer → Logic → Data
         ↕              ↕
      Security       Async
         ↕
       Utility
```

**STRICT LAYERING RULE**: View never imports Data. Logic never imports View.
Async accesses Logic (not Data) for state updates. No exceptions.

## Package map — base: `com.serviplus.apicontabilidad`

| Package         | Role                                                                           |
| --------------- | ------------------------------------------------------------------------------ |
| `.view`         | REST controllers (`*ViewSet`)                                                  |
| `.serializer.*` | Java records (Request/Response) + Serializer static mappers                    |
| `.logic`        | Services (CotizacionService, FacturaService, PagoService, PDFGeneratorService) |
| `.data`         | Spring Data JPA repositories                                                   |
| `.async`        | Event listeners (EmailCotizacionTask, PDFGeneratorTask)                        |
| `.async.event`  | ApplicationEvent subclasses                                                    |
| `.security`     | JwtFilter, SecurityConfig, SecureLogger                                        |
| `.utility`      | GlobalExceptionHandler, ApiError, NumeroGenerator, 3 custom exceptions         |
| `.config`       | AppProperties, AsyncConfig, MinioConfig, SwaggerConfig                         |
| `.domain`       | JPA entities + EstadoCotizacion/EstadoFactura enums                            |

## Domain entities

- **Cotizacion** — estados: `BORRADOR → ENVIADA → ACEPTADA / RECHAZADA → ANULADA`
- **Factura** — estados: `PENDIENTE → PAGADA / ANULADA`
- **Abono** — partial payment that reduces `factura.saldo`
- **AuditLog** — write-only (ADR-S3), never update or delete rows
- **Contador** — pessimistic-locked sequential number (COT-YYYY-NNNN / FAC-YYYY-NNNN)

## API endpoints (what the frontend calls)

| Method | Path                               | Auth           |
| ------ | ---------------------------------- | -------------- |
| GET    | /api/v1/cotizaciones               | Any auth'd     |
| POST   | /api/v1/cotizaciones               | Any auth'd     |
| GET    | /api/v1/cotizaciones/{id}          | Any auth'd     |
| PUT    | /api/v1/cotizaciones/{id}/aprobar  | ADMIN/CONTADOR |
| PUT    | /api/v1/cotizaciones/{id}/rechazar | ADMIN/CONTADOR |
| GET    | /api/v1/facturas/{id}              | Any auth'd     |
| POST   | /api/v1/facturas                   | Any auth'd     |
| GET    | /api/v1/facturas/{id}/abonos       | Any auth'd     |
| POST   | /api/v1/facturas/{id}/abonos       | ADMIN/CONTADOR |

## Business rules (enforce always)

- IVA = 13% (`app.iva.rate` property; never hardcode)
- All money → `BigDecimal` with `RoundingMode.HALF_UP`, scale 2
- State transitions validated by `EstadoCotizacion.puedeTransicionarA()` and `EstadoFactura.puedeTransicionarA()`
- `Factura` auto-transitions to `PAGADA` when `saldo` reaches `BigDecimal.ZERO` after an abono
- Every business action writes one row to `audit_log`
- JWT claims: `sub`=username, `roles`=[`"ROLE_ADMIN"`,`"ROLE_CONTADOR"`,`"ROLE_CLIENTE"`]
- `GET /api/v1/cotizaciones` returns ALL for ADMIN/CONTADOR, filtered by `creado_por` for CLIENTE

## Coding standards (enforced by team guidelines)

- **Commits**: `<tipo>: <descripción ≤72 chars en inglés>` — tipos: feat/fix/refactor/chore/test/docs/style
- **No comments** unless the WHY is non-obvious
- **Java records** for all DTOs (Request/Response) — immutable by design
- **Lombok** on entities: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- **SOLID**: Controllers depend on service class directly (Spring handles DIP via IoC)
- **No hardcoded values** — all config via `AppProperties` (bound from `.env`)
- **Validation** via Jakarta Bean Validation on all Request records

## Development workflow

### Branch naming

```
feature/HU-XX-short-description      ← new feature, branched from develop
hotfix/HU-XX-short-description       ← urgent fix, branched from main → merged into main AND develop
```

With role differentiation (when HUs are split by user type):

```
feature/HUC-XX-...   ← Cliente role
feature/HUF-XX-...   ← Funcionario role
feature/HUA-XX-...   ← Administrador role
```

### Branch base rules

| Branch type | Created from | Merges into  |
| ----------- | ------------ | ------------ |
| `feature/`  | `develop`    | `develop`    |
| `hotfix/`   | `main`       | `main` + `develop` |

Never branch from another feature branch without lead approval.

### PR checklist (open only when done + tested locally)

```markdown
## Historia de Usuario
Enlace: [HU-XX - Nombre](<link>)

## ¿Qué hace este PR?
Descripción concisa.

## Cambios Realizados
- [ ] Cambio 1

## Cómo Probar
1. Paso 1
2. Resultado esperado

## Checklist
- [ ] Compila sin errores
- [ ] Tests pasan localmente
- [ ] Sin console.log ni código comentado
- [ ] Rama actualizada con la base
```

### Commit rule
One commit per logical unit of work. Never go more than 2 hours without committing.
Description ≤ 72 chars. Never mix Spanish/English in the same project.

## Configuration — AppProperties structure

```
app.jwt.secret          → JWT_SECRET env var
app.iva.rate            → 0.13 (13% IVA)
app.minio.endpoint      → MINIO_ENDPOINT
app.minio.access-key    → MINIO_ACCESS_KEY
app.minio.secret-key    → MINIO_SECRET_KEY
app.minio.bucket        → MINIO_BUCKET
app.email.from          → EMAIL_FROM
app.cors.allowed-origins→ CORS_ALLOWED_ORIGINS (comma-separated)
```

## Flyway migrations

- Location: `backend/src/main/resources/db/migration/`
- Naming: `V{N}__{snake_case_description}.sql`
- Current: V1\_\_schema_inicial.sql (cotizaciones, lineas_cotizacion, facturas, lineas_factura, abonos, audit_log, contadores)

## Known limitations (don't fix unless explicitly asked)

1. Client email in `EmailCotizacionTask` is a placeholder → needs Clientes microservice
2. PDF layout in `PDFGeneratorService` is minimal → needs full template
3. Frontend `/login` route doesn't exist → auth flow not implemented
4. `spring-boot-starter-flyway` may need to be `flyway-core` if Maven can't resolve it
5. springdoc `2.8.3` targets Spring Boot 3.x → may need `3.x` for Spring Boot 4.x
6. Sessions (spring-boot-starter-session-jdbc) kept in pom.xml but disabled via `STATELESS` policy

## Frontend TypeScript rules

- All source files: `.tsx` (components, pages) or `.ts` (pure logic, types)
- Never use `any` — use `unknown` + type guard if type is truly unknown
- All API responses typed via `src/types.ts` — add types there if backend adds fields
- Axios calls: always generic `axios.get<Type>(...)` — never raw `.get()`
- Promises in event handlers: wrap with `void` or use a safe error handler — never ignore
- `valueAsNumber: true` in `register()` for all `type="number"` inputs
- `@testing-library/jest-dom` matchers are available in tests (setup.ts imports them)
- Prettier config: `frontend/.prettierrc` — semi, singleQuote, tabWidth 2, printWidth 100

## Test separation (backend)

| Command         | What runs                                                | When              |
| --------------- | -------------------------------------------------------- | ----------------- |
| `./mvnw test`   | `*Test.java` (Mockito, no DB)                            | Every commit      |
| `./mvnw verify` | `*Test.java` + `*IT.java` (Testcontainers) + JaCoCo gate | Manual / release  |

Integration tests live in `src/test/java/.../integration/` and extend `AbstractContainerIT`.
JwtTestHelper generates signed tokens for test requests.

## CI (GitHub Actions)

| Workflow           | Triggers          | Jobs                                                    |
| ------------------ | ----------------- | ------------------------------------------------------- |
| `pr-develop.yml`   | PR → develop only | backend-checks (unit + Sonar), frontend-checks (Prettier + TS + ESLint + Vitest + build) |

Required GitHub secrets/vars for Sonar:
- Secret `SONAR_TOKEN` — from sonarcloud.io
- Variable `SONAR_PROJECT_KEY` — e.g. `org_api-contabilidad`
- Variable `SONAR_ORGANIZATION` — e.g. `org-slug`

## What NOT to do

- Never access the DB repository directly from a controller
- Never skip the serializer layer (no raw entities in REST responses)
- Never use `@Transactional` in a view/controller class
- Never use `latest` Docker tags — all images must be pinned
- Never hardcode env-specific values — always use `${ENV_VAR:default}`
- Never open a PR without completing the PR checklist above
- Never merge without at least one approval

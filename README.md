# api-contabilidad — Yummy Inc

Módulo de contabilidad del sistema de gestión empresarial Yummy Inc.
Gestiona **Facturas**, **Cotizaciones** y **Pagos (Abonos)** a través de una API REST + SPA React.

**Architecture**: ADR-M1 (Strict Layered) · ADR-M2 (REST API) · ADR-M3 (Expanded MVC) · ADR-S1 (Docker Compose) · ADR-S3 (Audit Log)

---

## Prerequisites

| Tool | Minimum Version |
|------|-----------------|
| Docker | 27.x |
| Docker Compose | 2.x (plugin — use `docker compose`, not `docker-compose`) |
| Java (local dev) | 21 LTS |
| Maven (local dev) | 3.9.x (or use `./mvnw`) |
| Node.js (local dev) | 20.18 LTS |

---

## Quick Start (Docker)

```bash
# 1. Clone the repository
git clone <repo-url> api-contabilidad
cd api-contabilidad

# 2. Create your environment file from the template
cp .env.example .env
# Edit .env and fill in all required values (DB_PASSWORD, JWT_SECRET, etc.)

# 3. Start all services
docker compose up --build

# 4. Verify health
curl http://localhost:8080/actuator/health        # Backend Spring Boot
curl http://localhost:8000/api/v1/facturas        # Via Kong Gateway (requires JWT)
```

Once running:
- **Kong Gateway**: http://localhost:8000 (proxy) · http://localhost:8001 (admin)
- **Backend API**: http://localhost:8080 (direct, bypass Kong for local dev)
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Frontend**: http://localhost:8000 (via Kong)
- **MinIO Console**: http://localhost:9001

---

## Run Unit Tests (without Docker)

```bash
cd backend

# Run all tests
./mvnw test

# Run tests for a specific class
./mvnw test -Dtest=FacturaServiceTest

# Run integration tests only
./mvnw test -Dgroups=integration
```

---

## Generate Coverage Report (JaCoCo)

```bash
cd backend

# Run tests + generate report
./mvnw verify

# View the report (HTML)
# Open: backend/target/site/jacoco/index.html

# On macOS/Linux:
open target/site/jacoco/index.html

# On Windows:
start target/site/jacoco/index.html
```

Coverage enforcement:
- `com.yummy.contabilidad.logic.*` — **100% instruction coverage** required
- `com.yummy.contabilidad.utility.*` — **100% instruction coverage** required
- Build **fails** if coverage drops below these thresholds (JaCoCo `check` goal)

---

## Project Structure

```
api-contabilidad/
├── .env.example              ← Environment variable template (copy to .env)
├── .gitignore
├── docker-compose.yml        ← ADR-S1: Kong, Backend, Frontend, MariaDB, Redis, MinIO
├── README.md
│
├── backend/                  ← Spring Boot 3.3 (Java 21)
│   ├── Dockerfile             ← Multi-stage: eclipse-temurin:21-jdk-alpine → jre-alpine
│   ├── pom.xml
│   └── src/main/java/com/yummy/contabilidad/
│       ├── view/              ← VIEW LAYER: @RestController (FacturaController, etc.)
│       ├── serializer/        ← SERIALIZER LAYER: DTOs + MapStruct mappers
│       │   ├── dto/
│       │   └── mapper/
│       ├── logic/             ← LOGIC LAYER: @Service (FacturaService, etc.) ← main business logic
│       ├── domain/            ← DOMAIN: JPA entities + State enums
│       ├── data/              ← DATA LAYER: Spring Data JPA repositories
│       ├── async/             ← ASYNC LAYER: Events + @Async tasks (PDF, Email)
│       ├── security/          ← SECURITY LAYER: SecurityFilterChain, JWT, RBAC
│       ├── utility/           ← UTILITY LAYER: GlobalExceptionHandler, Validators
│       └── config/            ← Configuration beans (Redis, MinIO, Swagger, CORS, Async)
│
├── frontend/                 ← React 18 + Vite (Node 20)
│   ├── Dockerfile             ← Multi-stage: node:20.18-alpine → nginx:1.27-alpine
│   ├── nginx.conf
│   └── src/
│       ├── components/RouteGuard.jsx   ← JWT guard (Proxy pattern)
│       └── pages/                      ← 6 accounting pages
│
└── kong/
    └── kong.yml              ← Kong 3.8 DB-less: JWT + CORS + Rate Limiting + Logging
```

---

## Key Architectural Decisions

| ADR | Decision | Rationale |
|-----|----------|-----------|
| ADR-M1 | Strict Layered Architecture | Financial domain requires strict separation; no layer can access a non-adjacent layer |
| ADR-M2 | REST API (`/api/v1/`) | Interoperability with Ventas, Inventario, and Autenticación modules |
| ADR-M3 | Expanded MVC (7 layers) | Standard MVC insufficient for financial domain; View + Serializer + Logic + Data + Async + Security + Utility |
| ADR-S1 | Docker Compose | Isolated containers for each service; reproducible environments |
| ADR-S3 | `audit_log` table | Non-repudiation; all financial operations recorded with user, timestamp, before/after state |

---

## Design Patterns Implemented

| Pattern | Where |
|---------|-------|
| **State** | `EstadoFactura`, `EstadoCotizacion` — lifecycle transition validation |
| **Strategy** | `FacturaValidator`, `CotizacionValidator` — swappable validation algorithms |
| **Chain of Responsibility** | Bean Validation (HTTP) → Business validators → State transitions |
| **Facade** | `FacturaService`, `CotizacionService` — unified entry points |
| **Observer** | `ApplicationEventPublisher` + `@EventListener` → async tasks |
| **Command** | `AuditLog` entity — encapsulates who did what, on which entity, when |
| **Proxy** | `SecurityFilterChain` + `JwtAuthFilter` + `RouteGuard` |
| **Bridge** | `PDFGeneratorService` + `MinioClient` — swap MinIO for S3 without changing service |
| **Repository** | Spring Data JPA repositories in `data/` package |
| **DTO / Adapter** | MapStruct mappers in `serializer/mapper/` |
| **Template Method** | Service methods: validate → execute → audit → notify |
| **Flyweight** | HikariCP connection pool + Spring IoC singletons |
| **Builder** | `docker-compose.yml` + all `@Builder` domain entities |
| **Decorator** | `SecureLogger` wraps SLF4J |
| **Memento** | `captureSnapshot()` in services + `@Transactional` rollback |

---

## Known Limitations and Next Steps

1. **PDF generation**: `PDFGeneratorService` generates plain-text PDFs. Replace `generateContent()` with Apache PDFBox 3.x or iText 7 for production-quality PDFs.

2. **Email**: `EmailCotizacionTask` sends plain-text emails. Replace with Thymeleaf HTML templates.

3. **Redis async**: Current implementation uses in-process Spring `@Async` + `@EventListener`. For distributed deployments (multiple backend instances), replace with Redis pub/sub using `RedisMessageListenerContainer` — the task logic in `PDFGeneratorTask` and `EmailCotizacionTask` does not need to change.

4. **Document number generation**: `generarNumero()` uses a count-by-year approach. Under high concurrency, use a database sequence (`CREATE SEQUENCE`) instead.

5. **Client email**: `EmailCotizacionTask` uses a hardcoded placeholder email. Connect to the Clientes microservice to look up the client's actual email address.

6. **Kong JWT consumer**: `kong.yml` contains seed consumers with development keys. In production, the auth service creates consumers dynamically via the Kong Admin API.

7. **Frontend auth**: JWT stored in `localStorage`. For production, use `httpOnly` cookies issued by the auth service to prevent XSS-based theft.

8. **Test coverage**: The integration test (`FacturaTransactionalFlowTest`) requires `MockBean` for MinIO and JavaMailSender. Add a complete test configuration class (`@TestConfiguration`) with mocks for all external services.

# Contabilidad — Serviplus SA

![CI Backend](https://github.com/ServiPlus-S-A/api-contabilidad/actions/workflows/pr-develop.yml/badge.svg)
![CD Main](https://github.com/ServiPlus-S-A/api-contabilidad/actions/workflows/cd-main.yml/badge.svg)
![Coverage](https://img.shields.io/badge/coverage-80%25-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot)
![React](https://img.shields.io/badge/React-18.3-61DAFB?logo=react)
![MariaDB](https://img.shields.io/badge/MariaDB-11.4-003545?logo=mariadb)
![Kong](https://img.shields.io/badge/Kong-3.8-blue?logo=kong)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)

Accounting microservice for Serviplus SA. Manages **quotes (cotizaciones)**, **invoices (facturas)**, and **partial payments (abonos)** through a REST API + React SPA, deployed behind Kong Gateway.

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| API Gateway | Kong (DB-less) | 3.8 |
| Backend | Spring Boot + Java | 4.0.6 / 25 |
| Frontend | React + Vite | 18.3 / 5 |
| Database | MariaDB | 11.4 |
| Cache / Async | Redis | 7.4 |
| Object Storage | MinIO | 2024-12-18 |
| Migrations | Flyway | — |
| Auth | JWT (jjwt 0.12) | — |
| API Docs | springdoc OpenAPI | 2.8.3 |
| Containerization | Docker Compose | — |

---

## Architecture

Seven strict layers (ADR-M1, ADR-M3) — no layer may skip its neighbor:

```
View → Serializer → Logic → Data
           ↕              ↕
        Security       Async
           ↕
         Utility
```

| Layer | Package | Role |
|-------|---------|------|
| View | `.view` | REST controllers (`*ViewSet`) |
| Serializer | `.serializer.*` | Java records (Request/Response) + static mappers |
| Logic | `.logic` | Services — business rules, state transitions, audit |
| Data | `.data` | Spring Data JPA repositories |
| Async | `.async` | Event listeners (PDF generation, email) |
| Security | `.security` | JwtFilter, SecurityConfig, SecureLogger |
| Utility | `.utility` | GlobalExceptionHandler, custom exceptions |

---

## Environments

| Environment | URL | Trigger |
|-------------|-----|---------|
| Production | `http://<EC2_IP>:8000` | Push to `main` |
| Development | `http://<EC2_IP>:8100` | Push to `develop` |
| Local | `http://localhost:8000` | Manual |

---

## Quick Start

### Local (Docker)

```bash
git clone <repo-url> && cd api-contabilidad

cp .env.local.example .env.local
# Edit .env.local with your local values

docker compose -f docker-compose.yml -f docker-compose.local.yml \
  --env-file .env.local up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:8000 |
| Swagger UI | http://localhost:8000/swagger-ui/index.html |
| API Docs (JSON) | http://localhost:8000/v3/api-docs |
| MinIO Console | http://localhost:9001 |

### Backend only

```bash
cd backend
./mvnw test          # Unit tests (fast, no DB)
./mvnw verify        # Unit + integration tests + JaCoCo 80% gate
```

### Frontend only

```bash
cd frontend
npm install
npm run dev          # Dev server at http://localhost:5173
npm test             # Vitest unit tests
npm run test:coverage # Coverage report
```

---

## API Endpoints

| Method | Path | Auth |
|--------|------|------|
| GET | `/api/v1/cotizaciones` | Any authenticated |
| POST | `/api/v1/cotizaciones` | Any authenticated |
| GET | `/api/v1/cotizaciones/{id}` | Any authenticated |
| PUT | `/api/v1/cotizaciones/{id}/aprobar` | ADMIN / CONTADOR |
| PUT | `/api/v1/cotizaciones/{id}/rechazar` | ADMIN / CONTADOR |
| GET | `/api/v1/facturas/{id}` | Any authenticated |
| POST | `/api/v1/facturas` | Any authenticated |
| GET | `/api/v1/facturas/{id}/abonos` | Any authenticated |
| POST | `/api/v1/facturas/{id}/abonos` | ADMIN / CONTADOR |

Full interactive docs available via Kong at `/swagger-ui/index.html`.

> **Note:** Authentication (`POST /api/v1/auth/login`) is not yet implemented in this service — it is planned as a separate auth microservice.

---

## Project Structure

```
api-contabilidad/
├── .env.example              ← Production env template
├── .env.dev.example          ← Dev/staging env template
├── .env.local.example        ← Local dev env template
├── docker-compose.yml        ← Base services
├── docker-compose.dev.yml    ← Dev overrides (port 8100, contabilidad_dev DB)
├── docker-compose.local.yml  ← Local overrides (exposed ports, contabilidad_local DB)
├── kong/
│   ├── kong.yml              ← Production Kong config
│   └── kong.dev.yml          ← Dev Kong config
├── backend/                  ← Spring Boot 4 / Java 25
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/serviplus/apicontabilidad/
│       └── test/
└── frontend/                 ← React 18 / Vite
    ├── Dockerfile
    └── src/
        ├── pages/
        ├── components/
        └── __tests__/
```

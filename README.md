# Contabilidad — Serviplus SA

![CI Backend](https://github.com/ServiPlus-S-A/api-contabilidad/actions/workflows/pr-develop.yml/badge.svg)
![CD Main](https://github.com/ServiPlus-S-A/api-contabilidad/actions/workflows/cd-main.yml/badge.svg)
![Coverage](https://img.shields.io/badge/cobertura-80%25-brightgreen)
![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?logo=springboot)
![React](https://img.shields.io/badge/React-18.3-61DAFB?logo=react)
![MariaDB](https://img.shields.io/badge/MariaDB-11.4-003545?logo=mariadb)
![Kong](https://img.shields.io/badge/Kong-3.8-blue?logo=kong)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)

Microservicio de contabilidad para Serviplus SA. Gestiona **cotizaciones**, **facturas** y **abonos** a través de una API REST + SPA en React, desplegada detrás de Kong Gateway.

---

## Stack tecnológico

| Capa | Tecnología | Versión |
|------|-----------|---------|
| API Gateway | Kong (sin DB) | 3.8 |
| Backend | Spring Boot + Java | 4.0.6 / 25 |
| Frontend | React + Vite | 18.3 / 5 |
| Base de datos | MariaDB | 11.4 |
| Caché / Async | Redis | 7.4 |
| Almacenamiento de objetos | MinIO | 2024-12-18 |
| Migraciones | Flyway | — |
| Autenticación | JWT (jjwt 0.12) | — |
| Docs API | springdoc OpenAPI | 2.8.3 |
| Contenedores | Docker Compose | — |

---

## Arquitectura

Siete capas estrictas (ADR-M1, ADR-M3) — ninguna capa puede saltarse a su vecina:

```
View → Serializer → Logic → Data
           ↕              ↕
        Security       Async
           ↕
         Utility
```

| Capa | Paquete | Rol |
|------|---------|-----|
| View | `.view` | Controladores REST (`*ViewSet`) |
| Serializer | `.serializer.*` | Java records (Request/Response) + mappers estáticos |
| Logic | `.logic` | Servicios — reglas de negocio, transiciones de estado, auditoría |
| Data | `.data` | Repositorios Spring Data JPA |
| Async | `.async` | Listeners de eventos (generación PDF, correo) |
| Security | `.security` | JwtFilter, SecurityConfig, SecureLogger |
| Utility | `.utility` | GlobalExceptionHandler, excepciones personalizadas |

---

## Ambientes

| Ambiente | URL | Disparador |
|----------|-----|------------|
| Producción | `http://<EC2_IP>:8000` | Push a `main` |
| Desarrollo | `http://<EC2_IP>:8100` | Push a `develop` |
| Local | `http://localhost:8000` | Manual |

---

## Inicio rápido

### Local (Docker)

```bash
git clone <repo-url> && cd api-contabilidad

cp .env.local.example .env.local
# Editar .env.local con los valores locales

docker compose -f docker-compose.yml -f docker-compose.local.yml \
  --env-file .env.local up --build
```

| Servicio | URL |
|---------|-----|
| Frontend | http://localhost:8000 |
| Swagger UI | http://localhost:8000/swagger-ui/index.html |
| Docs API (JSON) | http://localhost:8000/v3/api-docs |
| MinIO Console | http://localhost:9001 |

> **Nota:** Swagger UI se enruta a través de Kong solo en ambientes local y dev. No está expuesto en producción.

### Solo backend

```bash
cd backend
./mvnw test          # Pruebas unitarias (rápido, sin DB)
./mvnw verify        # Pruebas unitarias + integración + gate JaCoCo 80%
```

### Solo frontend

```bash
cd frontend
npm install
npm run dev          # Servidor de desarrollo en http://localhost:5173
npm test             # Pruebas unitarias Vitest
npm run test:coverage # Reporte de cobertura
```

---

## Endpoints de la API

| Método | Ruta | Autenticación |
|--------|------|---------------|
| GET | `/api/v1/cotizaciones` | Cualquier usuario autenticado |
| POST | `/api/v1/cotizaciones` | Cualquier usuario autenticado |
| GET | `/api/v1/cotizaciones/{id}` | Cualquier usuario autenticado |
| PUT | `/api/v1/cotizaciones/{id}/aprobar` | ADMIN / CONTADOR |
| PUT | `/api/v1/cotizaciones/{id}/rechazar` | ADMIN / CONTADOR |
| GET | `/api/v1/facturas/{id}` | Cualquier usuario autenticado |
| POST | `/api/v1/facturas` | Cualquier usuario autenticado |
| GET | `/api/v1/facturas/{id}/abonos` | Cualquier usuario autenticado |
| POST | `/api/v1/facturas/{id}/abonos` | ADMIN / CONTADOR |

Documentación interactiva completa disponible vía Kong en `/swagger-ui/index.html`.

> **Nota:** La autenticación (`POST /api/v1/auth/login`) aún no está implementada en este servicio — está planificada como microservicio de autenticación independiente.

---

## Estructura del proyecto

```
api-contabilidad/
├── .env.example              ← Plantilla de variables de producción
├── .env.dev.example          ← Plantilla de variables dev/staging
├── .env.local.example        ← Plantilla de variables para desarrollo local
├── docker-compose.yml        ← Servicios base
├── docker-compose.dev.yml    ← Overrides dev (puerto 8100, BD contabilidad_dev)
├── docker-compose.local.yml  ← Overrides local (puertos expuestos, BD contabilidad_local)
├── kong/
│   ├── kong.yml              ← Configuración Kong producción
│   └── kong.dev.yml          ← Configuración Kong dev
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

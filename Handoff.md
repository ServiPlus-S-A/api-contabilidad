# Handoff.md — api-contabilidad

Documento de continuidad para retomar el proyecto sin re-leer el código.
**Última actualización**: 2026-06-16

---

## Estado actual del proyecto

### ✅ Completo
| Área | Detalle |
|------|---------|
| Frontend | Todas las páginas implementadas (cotizaciones + facturas). NO tocar a menos que se pida. |
| Backend — domain | Entidades JPA, enums con State Pattern, todas las relaciones mapeadas |
| Backend — data | 5 repositorios Spring Data JPA; ContadorRepository con `@Lock(PESSIMISTIC_WRITE)` |
| Backend — serializer | Java records para Request/Response; Serializers estáticos (sin MapStruct) |
| Backend — logic | CotizacionService, FacturaService, PagoService, PDFGeneratorService completos |
| Backend — view | CotizacionViewSet, FacturaViewSet, AbonoViewSet; RBAC con `@PreAuthorize` |
| Backend — security | JwtFilter (JJWT 0.12.x), SecurityConfig stateless, SecureLogger |
| Backend — async | EmailCotizacionTask, PDFGeneratorTask con @Async + Spring Events |
| Backend — utility | GlobalExceptionHandler, 3 excepciones tipadas, NumeroGenerator |
| Backend — config | AppProperties (@ConfigurationProperties), AsyncConfig, MinioConfig, SwaggerConfig |
| Infraestructura | Dockerfile (multi-stage, Java 25), kong.yml, V1__schema_inicial.sql, application.properties |
| Frontend TypeScript | Migración completa JS→TS strict; tsconfig.app.json strict; src/types.ts con todos los tipos de API |
| Frontend unit tests | Vitest + RTL: RouteGuard (2), MisCotizacionesPage (5), NuevaCotizacionPage (3) = 10 tests |
| Frontend E2E | Playwright Chromium: auth.spec (2), cotizaciones.spec (5), facturas.spec (4) = 11 tests; API mocked |
| Backend unit tests | 16 tests Mockito: CotizacionServiceTest (8), FacturaServiceTest (5), PagoServiceTest (6) |
| Backend integration | Testcontainers MariaDB 1.21.1 (BOM explícito en pom.xml): ApplicationContextIT, CotizacionIT, FacturaIT |
| CI GitHub Actions | `pr-develop.yml` — corre solo en PRs a `develop`; jobs: backend-checks + frontend-checks |
| Prettier | Frontend: prettier 3.4.2 + eslint-config-prettier 9.1.0; config en `frontend/.prettierrc` |
| Lineamientos | Documento completo en sección final de este archivo; resumen en CLAUDE.md |
| .gitattributes | `* text=auto` en raíz; CRLF normalizado a LF en commit |

### ❌ Pendiente / No implementado
| Área | Qué falta | Prioridad |
|------|-----------|-----------|
| Frontend | Página `/login` — RouteGuard redirige ahí pero no existe | Alta |
| Email | Email real del cliente (hardcoded placeholder en EmailCotizacionTask) | Media |
| PDF | Layout completo con tabla de líneas de detalle | Media |
| Kong JWT | Consumers con JWT credentials para producción | Baja |
| Rama develop | Creada — el CI se activa abriendo PRs hacia ella | ✅ |

---

## Decisiones arquitectónicas clave

### Por qué Java records para DTOs
Inmutabilidad garantizada en tiempo de compilación. Los `Request` records llevan anotaciones
de validación Bean Validation directamente en los parámetros del constructor.

### Por qué no MapStruct
Reducción de dependencias. Los Serializers son clases `final` con constructor privado y un
único método estático `toResponse(Entity)`. Son 100% testeables sin mocks.

### Por qué NumeroGenerator con `SERIALIZABLE`
El README documenta que count-by-year tiene riesgo de colisión bajo concurrencia alta.
Se mitiga con `@Transactional(isolation = Isolation.SERIALIZABLE)` + `@Lock(PESSIMISTIC_WRITE)`
en `ContadorRepository`. La columna `numero` tiene constraint `UNIQUE` como safety net.

### Por qué JWT en backend Y en Kong
Kong valida JWT en producción (segunda línea de defensa). El backend valida JWT directamente
para que el sistema funcione sin Kong en desarrollo local (hit directo a puerto 8080).

### Por qué @Async en lugar de Redis pub/sub
README lo documenta como limitación conocida. Para escalar, reemplazar `AsyncConfig` con un
Redis Streams consumer. Los `@EventListener` en las tasks ya están desacoplados del servicio
via `ApplicationEventPublisher` — el refactor sería solo en la capa async.

### Por qué Testcontainers BOM explícito en pom.xml
Spring Boot 4.0.x gestiona testcontainers vía su propio BOM, pero m2e (Eclipse Maven plugin)
no lo resuelve correctamente en el IDE, causando error "version cannot be empty". Se añadió
`<dependencyManagement>` con el BOM de testcontainers 1.21.1 explícitamente para que tanto
el IDE como el CLI resuelvan la versión sin problemas.

---

## Flujo transaccional de referencia

### POST /api/v1/cotizaciones
```
CotizacionViewSet.crear(request, auth)
  └─ CotizacionService.crear(request, usuario)
       ├─ NumeroGenerator.siguiente("COT")        [TX SERIALIZABLE → contadores table]
       ├─ buildLineas(request.lineas())           [BigDecimal arithmetic]
       ├─ Cotizacion.builder()...build()
       ├─ cotizacionRepository.save(cotizacion)   [TX principal]
       ├─ auditLogRepository.save(auditLog)       [ADR-S3]
       └─ CotizacionSerializer.toResponse(saved)  [never return raw entity]
→ HTTP 201 + CotizacionResponse
```

### PUT /api/v1/cotizaciones/{id}/aprobar
```
CotizacionViewSet.aprobar(id, auth)
  └─ CotizacionService.aprobar(id, usuario)
       ├─ cotizacionRepository.findById(id)       [404 si no existe]
       ├─ EstadoCotizacion.puedeTransicionarA()   [422 si transición inválida]
       ├─ cotizacion.setEstado(ACEPTADA)
       ├─ cotizacionRepository.save()
       ├─ auditLogRepository.save()
       └─ eventPublisher.publishEvent(CotizacionAprobadaEvent)
            └─ [async] EmailCotizacionTask.onCotizacionAprobada()
                 └─ mailSender.send(SimpleMailMessage)
→ HTTP 200 + CotizacionResponse
```

### POST /api/v1/facturas/{id}/abonos
```
AbonoViewSet.registrar(facturaId, request, auth)
  └─ PagoService.registrarAbono(facturaId, request, usuario)
       ├─ validarMontoPositivo()                  [400 si ≤ 0]
       ├─ validarFacturaPendiente()               [400 si no PENDIENTE]
       ├─ validarMontoNoSuperaSaldo()             [400 si monto > saldo]
       ├─ factura.setSaldo(saldo - monto)
       ├─ if saldo == 0 → factura.setEstado(PAGADA)
       ├─ facturaRepository.save()
       ├─ abonoRepository.save()
       └─ auditLogRepository.save()
→ HTTP 201 + AbonoResponse
```

---

## Estructura de carpetas

```
api-contabilidad/
├── CLAUDE.md                    ← Claude Code lee esto cada sesión
├── Handoff.md                   ← este archivo
├── .gitattributes               ← text=auto; LF normalizado en commit
├── docker-compose.yml
├── .env.example
├── .gitignore
├── README.md
├── .github/workflows/
│   └── pr-develop.yml           ← único CI; corre solo en PRs a develop
├── kong/
│   └── kong.yml                 ← Kong DB-less: CORS + rate limiting
├── backend/
│   ├── Dockerfile               ← multi-stage: eclipse-temurin:25
│   ├── pom.xml                  ← Spring Boot 4.0.6, Java 25, JaCoCo, Sonar, TC BOM
│   └── src/main/java/com/serviplus/apicontabilidad/
│       ├── domain/   data/   serializer/   logic/   view/
│       ├── async/   security/   utility/   config/
│   └── src/main/resources/
│       ├── application.properties
│       └── db/migration/V1__schema_inicial.sql
└── frontend/
    ├── .prettierrc              ← semi, singleQuote, tabWidth 2, printWidth 100
    ├── .prettierignore
    ├── eslint.config.js         ← eslint-config-prettier al final
    ├── package.json
    └── src/pages/               ← 6 páginas listas
```

---

## Variables de entorno requeridas (.env desde .env.example)

```bash
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, DB_ROOT_PASSWORD, DB_POOL_SIZE
REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET
JWT_SECRET          # mínimo 32 chars para HS256
JWT_EXPIRATION
SERVER_PORT         # default 8080
LOG_LEVEL, JPA_SHOW_SQL
CORS_ALLOWED_ORIGINS
EMAIL_FROM, SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD
VITE_API_BASE_URL   # para el build del frontend en Docker
KONG_PROXY_PORT, KONG_ADMIN_PORT
```

---

## Próximas tareas (en orden de prioridad)

1. **Crear rama `develop`** en GitHub y configurar como rama base para PRs
2. **Configurar SonarCloud** — crear proyecto en sonarcloud.io, agregar secret `SONAR_TOKEN` y variables `SONAR_PROJECT_KEY` / `SONAR_ORGANIZATION` en GitHub
3. **Página de login** (`frontend/src/pages/LoginPage.tsx`)
4. **Cotizacion enviar** — `PUT /api/v1/cotizaciones/{id}/enviar` (BORRADOR → ENVIADA)
5. **PDF layout completo** — tabla de líneas en `PDFGeneratorService.generarPDF()`

---

## Lineamientos de Desarrollo del Equipo

**Versión:** 1.0 — **Fecha:** 2026-05-31 — **Audiencia:** Ingenieros Junior

### 1. Flujo Estándar de Desarrollo

```
1. Leer la HU en Azure DevOps
2. Aclarar dudas ANTES de escribir código
3. Crear rama desde la rama base correcta
4. Commits pequeños y frecuentes (máx. 2h sin commit)
5. Abrir PR con la plantilla completa
6. Actualizar Azure DevOps con evidencias
7. Solicitar revisión de código
8. Merge solo después de aprobación
```

**Principios:**
- Legibilidad primero. Consistencia con el patrón ya establecido.
- No hacer más de lo pedido. No dejar código comentado.
- El historial de git reemplaza los comentarios de "qué se eliminó".

### 2. Convención de Commits

**Formato:** `<tipo>: <descripción corta en imperativo>` — máx. 72 chars

| Tipo | Cuándo |
|------|--------|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de bug |
| `refactor` | Reestructuración sin cambio de comportamiento |
| `chore` | Mantenimiento, config, dependencias |
| `test` | Agregar o modificar pruebas |
| `docs` | Documentación |
| `style` | Formato sin cambio de lógica |

**Correctos:**
```bash
feat: added user authentication endpoint
fix: removed null pointer on payment validation
chore: added eslint configuration
```

**Incorrectos:**
```bash
fix: correcciones           # muy vago
update                      # sin tipo, sin descripción
feat: added login, fixed dashboard bug, updated styles   # demasiado, dividir
```

### 3. Ramas y Pull Requests

**Nomenclatura:**
```
feature/HU-XX-nombre-corto       ← desde develop, hacia develop
feature/HUC-XX-nombre            ← HU de rol Cliente
feature/HUF-XX-nombre            ← HU de rol Funcionario
hotfix/HU-XX-nombre              ← desde main, hacia main + develop
```

**Reglas de PR:**
- Abrirlo solo cuando el desarrollo está **completo y probado localmente**.
- Asignar revisor y vincular el PR a la tarea de Revisión en Azure DevOps (sección *Development → Add link → GitHub Pull Request*).
- Responder comentarios de revisión dentro de 24 horas hábiles.
- No hacer merge propio sin aprobación.

### 4. Gestión del Tiempo y Escalamiento

**Regla de los 30 minutos:** si llevas más de 30 min bloqueado sin avance, escala.

**Cómo escalar** (preparar antes de preguntar):
1. ¿Qué intento hacer?
2. ¿Qué ya intenté?
3. ¿Cuál es el error exacto?
4. ¿Cuál es mi hipótesis?

**Alertas tempranas:** avisar al líder al 50% del tiempo si no hay progreso visible.

### 5. Azure DevOps — Documentación por Tarea

Cuatro elementos obligatorios antes de cerrar una HU:

| # | Elemento | Responsable | Contenido |
|---|----------|-------------|-----------|
| 1 | Requerimiento | Líder/PO | HU, criterios, mockups — no modificar |
| 2 | Desarrollo | Ingeniero | Link a la rama en GitHub (Development → Add link → GitHub Branch) |
| 3 | Plan de Pruebas | Ingeniero | Excel `HU_XX_Nombre - MP.xlsx` adjunto en tarea *Plan de pruebas* |
| 4 | Evidencia | Ingeniero | Doc con capturas por criterio: qué valida, qué se observó, PASA/FALLA |

**Estados del tablero:**
```
Nueva → En Progreso → En Revisión → En Pruebas → Cerrada (solo el líder cierra)
```

### 6. Checklist de Cierre de Tarea

```
Desarrollo
- [ ] Cumple exactamente los criterios de aceptación
- [ ] Sin console.log, código comentado, TODOs pendientes
- [ ] Rama actualizada con la base (develop o main)
- [ ] Commits frecuentes bajo la convención

Pull Request
- [ ] PR abierto con plantilla completa
- [ ] Revisor asignado
- [ ] PR vinculado a la HU en Azure DevOps

Azure DevOps
- [ ] Link a la rama en la HU (sección Desarrollo)
- [ ] Link al plan de pruebas (sección Plan de Pruebas)
- [ ] Evidencias con capturas (sección Pruebas)
- [ ] Tarea en el estado correcto del tablero
```

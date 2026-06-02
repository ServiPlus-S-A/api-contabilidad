# Handoff.md — api-contabilidad

Documento de continuidad para retomar el proyecto sin re-leer el código.
**Última actualización**: 2026-06-01

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
| Tests | 16 unit tests: CotizacionServiceTest (8), FacturaServiceTest (5), PagoServiceTest (6) |

### ❌ Pendiente / No implementado
| Área | Qué falta | Prioridad |
|------|-----------|-----------|
| Frontend | Página `/login` — RouteGuard redirige ahí pero no existe | Alta |
| Email | Email real del cliente (hardcoded placeholder en EmailCotizacionTask) | Media |
| PDF | Layout completo con tabla de líneas de detalle | Media |
| Kong JWT | Consumers con JWT credentials para producción | Baja |
| CRLF | Agregar `.gitattributes` raíz con `* text=auto` | Baja |

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

## Estructura de carpetas generada

```
api-contabilidad/
├── CLAUDE.md                    ← Claude Code lee esto cada sesión
├── Handoff.md                   ← este archivo
├── docker-compose.yml
├── .env.example
├── .gitignore
├── README.md
├── kong/
│   └── kong.yml                 ← Kong DB-less: CORS + rate limiting
├── backend/
│   ├── Dockerfile               ← multi-stage: eclipse-temurin:25
│   ├── pom.xml                  ← Spring Boot 4.0.6, Java 25, JaCoCo
│   └── src/main/java/com/serviplus/apicontabilidad/
│       ├── BackendApplication.java   ← @EnableConfigurationProperties(AppProperties)
│       ├── domain/                   ← JPA entities + enums
│       ├── data/                     ← Repositories
│       ├── serializer/               ← DTOs (records) + static mappers
│       │   ├── cotizacion/
│       │   ├── factura/
│       │   └── abono/
│       ├── logic/                    ← Services (reglas de negocio)
│       ├── view/                     ← REST controllers
│       ├── async/                    ← @EventListener tasks
│       │   └── event/                ← ApplicationEvent subclasses
│       ├── security/                 ← JWT filter + config
│       ├── utility/                  ← Handler global, excepciones, NumeroGenerator
│       └── config/                   ← AppProperties, MinioConfig, etc.
│   └── src/main/resources/
│       ├── application.properties
│       └── db/migration/V1__schema_inicial.sql
└── frontend/                    ← React 18 + Vite (NO TOCAR sin instrucción)
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

## Próximas tareas sugeridas (en orden de prioridad)

1. **Página de login** (`frontend/src/pages/LoginPage.jsx`)
   - POST a un endpoint de auth (externo, o temporal en backend)
   - Guardar `access_token` en localStorage
   - Redirigir a `/cotizaciones`

2. **Cotizacion enviar** — falta el botón "Enviar para revisión" en `DetalleCotizacionPage`
   - Endpoint: `PUT /api/v1/cotizaciones/{id}/enviar` (estado BORRADOR → ENVIADA)
   - Agregar método `enviar()` en `CotizacionService` y ruta en `CotizacionViewSet`

3. **Tests de integración** — `@SpringBootTest` con Testcontainers MariaDB
   - Reemplazar `BackendApplicationTests` (context loads) con integración real

4. **PDF layout completo** — tabla de líneas en `PDFGeneratorService.generarPDF()`

5. **Lint de frontend** — `cd frontend && npm run lint`

---

## Convención de commits del proyecto
```
feat: descripción corta ≤ 72 chars en inglés
fix: ...
refactor: ...
chore: ...
test: ...
docs: ...
style: ...
```
Nunca mezclar español/inglés en el mismo proyecto.

# Contribuir a api-contabilidad

## Nomenclatura de ramas

Cada integrante trabaja en su **rama personal** nombrada con las dos iniciales de su nombre completo, guion bajo y apellido:

```
jm_rodriguez   ← Juan M. Rodríguez
ja_ochoa       ← J. A. Ochoa
ja_ortiz       ← J. A. Ortiz
```

| Tipo de rama | Se crea desde | Merge hacia |
|---|---|---|
| Personal (`iniciales_apellido`) | `develop` | `develop` |
| `hotfix/HU-XX-descripcion` | `main` | `main` + `develop` |

Solo `develop` puede abrir un PR hacia `main`. Las ramas personales nunca van directo a `main`.

---

## Formato de commits

```
<tipo>: <descripción ≤72 caracteres en español>
```

| Tipo | Cuándo usarlo |
|------|---------------|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de bug |
| `refactor` | Cambio de código sin cambio de comportamiento |
| `test` | Agregar o actualizar pruebas |
| `chore` | Build, CI, configuración, dependencias |
| `docs` | Solo documentación |
| `style` | Formato, lint |

Un commit por unidad lógica de trabajo. Nunca más de 2 horas sin commitear.

---

## Flujo de trabajo

```bash
# 1. Partir siempre desde develop actualizado
git checkout develop && git pull origin develop
git checkout iniciales_apellido   # tu rama personal (ej: jm_rodriguez)
git merge develop                 # incorporar los últimos cambios

# 2. Trabajar y commitear seguido
git add <archivos>
git commit -m "feat: agregar X a Y"

# 3. Push y abrir PR apuntando a develop
git push origin iniciales_apellido
gh pr create --base develop
```

---

## Antes de abrir un PR

Ejecutar todas las verificaciones localmente:

```bash
# Backend
cd backend
./mvnw test          # Pruebas unitarias
./mvnw verify        # Pruebas de integración + gate JaCoCo 80%

# Frontend
cd frontend
npm run format:check # Prettier
npm run lint         # ESLint
npm run type-check   # TypeScript
npm test             # Vitest (29 tests, 80%+ cobertura)
```

Todos deben pasar. El CI ejecuta las mismas verificaciones y bloqueará el merge ante cualquier fallo.

---

## Plantilla de PR

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
- [ ] Rama actualizada con develop
```

Los PRs deben escribirse en español. Qodo revisa cada PR automáticamente — atender todos los hallazgos marcados como "Action required" antes de solicitar revisión humana.

---

## Estándares de código

- **Sin comentarios** a menos que el POR QUÉ no sea obvio
- **Java records** para todos los DTOs — inmutables por diseño
- **Lombok** en entidades: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- **Sin valores hardcodeados** — toda configuración vía `AppProperties` (desde `.env`)
- **Capas estrictas**: View nunca importa Data; Logic nunca importa View
- **BigDecimal** para todo lo monetario, `RoundingMode.HALF_UP`, escala 2
- **Nunca** usar `any` en TypeScript — usar `unknown` + type guard
- **Nunca** commitear `.env`, `.env.local`, `.env.dev`

---

## Ejecutar pruebas específicas

```bash
# Backend — clase de prueba unitaria individual
./mvnw test -Dtest=CotizacionServiceTest

# Backend — prueba de integración individual
./mvnw verify -Dit.test=CotizacionIT

# Frontend — archivo de prueba individual
npx vitest run src/__tests__/LoginPage.test.tsx

# Frontend — E2E (sin backend, API mockeada)
npm run test:e2e
```

# Contributing to api-contabilidad

## Branch naming

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

## Commit format

```
<type>: <description ≤72 chars in English>
```

| Type | When to use |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Code change without behavior change |
| `test` | Adding or updating tests |
| `chore` | Build, CI, config, dependencies |
| `docs` | Documentation only |
| `style` | Formatting, lint |

One commit per logical unit of work. Never go more than 2 hours without committing.

---

## Workflow

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

## Before opening a PR

Run all checks locally:

```bash
# Backend
cd backend
./mvnw test          # Unit tests
./mvnw verify        # Integration tests + 80% JaCoCo gate

# Frontend
cd frontend
npm run format:check # Prettier
npm run lint         # ESLint
npm run type-check   # TypeScript
npm test             # Vitest (29 tests, 80%+ coverage)
```

All must pass. CI runs the same checks and will block merge on failure.

---

## PR template

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

## Coding standards

- **No comments** unless the WHY is non-obvious
- **Java records** for all DTOs — immutable by design
- **Lombok** on entities: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- **No hardcoded values** — all config via `AppProperties` (bound from `.env`)
- **Strict layering**: View never imports Data; Logic never imports View
- **BigDecimal** for all money, `RoundingMode.HALF_UP`, scale 2
- **Never** use `any` in TypeScript — use `unknown` + type guard
- **Never** commit `.env`, `.env.local`, `.env.dev`

---

## Running specific tests

```bash
# Backend — single unit test class
./mvnw test -Dtest=CotizacionServiceTest

# Backend — single integration test
./mvnw verify -Dit.test=CotizacionIT

# Frontend — single test file
npx vitest run src/__tests__/LoginPage.test.tsx

# Frontend — E2E (no backend needed, API mocked)
npm run test:e2e
```

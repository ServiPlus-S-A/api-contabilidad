# Contributing to api-contabilidad

## Branch naming

```
feature/HU-XX-short-description      ← new feature, from develop
hotfix/HU-XX-short-description       ← urgent fix, from main → merged into main AND develop
```

With role differentiation:

```
feature/HUC-XX-...   ← Cliente role
feature/HUF-XX-...   ← Funcionario role
feature/HUA-XX-...   ← Administrador role
```

| Branch type | Created from | Merges into |
|-------------|-------------|-------------|
| `feature/` | `develop` | `develop` |
| `hotfix/` | `main` | `main` + `develop` |

Never branch from another feature branch without lead approval.

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
# 1. Branch from develop
git checkout develop && git pull origin develop
git checkout -b feature/HU-XX-my-feature

# 2. Work, commit often
git add <files>
git commit -m "feat: add X to Y"

# 3. Push and open PR targeting develop
git push origin feature/HU-XX-my-feature
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
## Summary
Briefly explain what this PR does and why.

## Changes
- Main change 1
- Main change 2

## Notes
(optional) Anything reviewers should be aware of.
```

PRs must be written in English. Qodo reviews every PR automatically — address all "Action required" findings before requesting human review.

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

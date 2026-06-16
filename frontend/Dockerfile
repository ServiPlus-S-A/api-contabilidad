# =============================================================================
# api-contabilidad frontend Dockerfile — Multi-stage build (Rule R5)
# Stage 1 (builder): Node.js — npm install + vite build
# Stage 2 (runtime): nginx-unprivileged — serves the static SPA as non-root
# =============================================================================

# ── Stage 1: Builder ──────────────────────────────────────────────────────────
FROM node:20.18-alpine3.20 AS builder

WORKDIR /app

# Copy package files first (layer cache: npm install only re-runs when package.json changes)
COPY package.json package-lock.json ./
RUN npm ci --frozen-lockfile

# Build arg for the API base URL (injected at build time by docker-compose)
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY . .
RUN npm run build

# ── Stage 2: Runtime (nginx-unprivileged — non-root, port 8080 by default) ───
FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]

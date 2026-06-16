# =============================================================================
# api-contabilidad frontend Dockerfile — Multi-stage build (Rule R5)
# Stage 1 (builder): Node.js — npm install + vite build
# Stage 2 (runtime): Nginx — serves the static SPA bundle
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

# ── Stage 2: Runtime (Nginx) ──────────────────────────────────────────────────
FROM nginx:1.27-alpine3.20 AS runtime

# Security: run as non-root user (nginx group is created by the image)
RUN addgroup -S contabilidad-ui && \
    adduser -S contabilidad-ui -G contabilidad-ui

# Copy the built SPA and nginx config
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Nginx needs to write to these paths; adjust ownership
RUN chown -R contabilidad-ui:contabilidad-ui \
    /usr/share/nginx/html \
    /var/cache/nginx \
    /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown contabilidad-ui:contabilidad-ui /var/run/nginx.pid

USER contabilidad-ui

EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]

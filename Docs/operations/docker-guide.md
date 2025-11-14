# OpenL Tablets Docker Guide

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
**Base Image**: eclipse-temurin:25-jre-alpine

---

## Table of Contents

- [Overview](#overview)
- [Docker Image Architecture](#docker-image-architecture)
- [Quick Start](#quick-start)
- [Docker Compose Setup](#docker-compose-setup)
- [Building Docker Images](#building-docker-images)
- [Configuration](#configuration)
- [Volumes and Persistence](#volumes-and-persistence)
- [Networking](#networking)
- [Security](#security)
- [Performance Tuning](#performance-tuning)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

---

## Overview

OpenL Tablets provides official Docker images for containerized deployments with:

- **Multi-stage build**: Optimized image size
- **Non-root user**: Security best practice (UID 1000)
- **OpenTelemetry integration**: Built-in observability
- **Jetty 12**: Modern embedded web server
- **Alpine Linux**: Minimal base image

### Official Images

| Image | Purpose | Base |
|-------|---------|------|
| `openltablets/webstudio` | Web Studio (Design) | Eclipse Temurin 25 JRE Alpine |
| `openltablets/ws` | Rule Services (Runtime) | Eclipse Temurin 25 JRE Alpine |

---

## Docker Image Architecture

### Multi-Stage Build

The Dockerfile uses a multi-stage build for optimization:

```dockerfile
# Stage 1: Download and verify OpenTelemetry agent
FROM alpine AS otel
ENV OTEL_VER 2.20.1
RUN apk add --no-cache wget gnupg && \
    wget -O opentelemetry-javaagent.jar ... && \
    gpg --verify opentelemetry-javaagent.jar.asc

# Stage 2: Build application image
FROM eclipse-temurin:25-jre-alpine AS openl
COPY --from=otel opentelemetry-javaagent.jar /opt/opentelemetry/
COPY --from=jetty:12.0-jre21 /usr/local/jetty /opt/openl/app
```

### Directory Structure

```
/opt/openl/
├── app/                    # Jetty installation
│   ├── start.jar          # Jetty launcher
│   └── webapps/ROOT/      # OpenL application
├── lib/                    # External JARs (JDBC drivers, etc.)
├── local/                  # Local workspace (ephemeral)
├── shared/                 # Shared workspace (persistent)
├── logs/                   # Application logs
├── start.sh               # Startup script
└── setenv.sh              # Environment configuration

/opt/opentelemetry/
├── opentelemetry-javaagent.jar
└── openl-rules-opentelemetry.jar
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENL_DIR` | `/opt/openl` | OpenL installation directory |
| `OPENL_HOME` | `/opt/openl/local` | Local workspace |
| `OPENL_HOME_SHARED` | `/opt/openl/shared` | Shared workspace (mount this!) |
| `OPENL_APP` | `/opt/openl/app` | Jetty application directory |
| `OPENL_LIB` | `/opt/openl/lib` | External libraries directory |
| `OTEL_DIR` | `/opt/opentelemetry` | OpenTelemetry directory |
| `OTEL_SERVICE_NAME` | `OpenL` | Service name for tracing |
| `JAVA_OPTS` | `-Xms32m -XX:MaxRAMPercentage=90.0` | JVM options |

---

## Quick Start

### Using Docker

```bash
# Run Web Studio
docker run -d \
  --name openl-studio \
  -p 8080:8080 \
  -v openl-data:/opt/openl/shared \
  openltablets/webstudio:latest

# Access at http://localhost:8080
# Default credentials: admin / admin
```

### Using Docker Compose

```bash
# Clone repository
git clone https://github.com/openl-tablets/openl-tablets.git
cd openl-tablets

# Build and start services
docker compose up -d

# Access services:
# - Web Studio: http://localhost/studio (admin/admin)
# - Rule Services: http://localhost/services
# - Proxy: http://localhost
```

---

## Docker Compose Setup

### Architecture

The `compose.yaml` defines a complete OpenL Tablets stack:

```
┌─────────────────────────────────────────┐
│  Nginx Proxy (:80, :443)                │
│  ├─ /studio/  → studio:8080             │
│  └─ /services/ → services:8080          │
└─────────────────┬───────────────────────┘
                  │
    ┌─────────────┼─────────────┐
    │             │             │
    ▼             ▼             ▼
┌─────────┐  ┌──────────┐  ┌──────────┐
│ Studio  │  │ Services │  │PostgreSQL│
│ :8080   │  │ :8081    │  │ :5432    │
└─────────┘  └──────────┘  └──────────┘
```

### Services

#### 1. Web Studio (studio)

```yaml
studio:
  build:
    context: .
    args:
      APP: STUDIO/org.openl.rules.webstudio/target/webapp
  image: openltablets/webstudio
  ports:
    - "8080:8080"   # HTTP
    - "5005:5005"   # Debug
  volumes:
    - jars:/opt/openl/lib
    - openl-home:/opt/openl/shared
  environment:
    JAVA_OPTS: '-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005'
  deploy:
    resources:
      limits:
        cpus: '2.0'
        memory: 1G
      reservations:
        memory: 512M
```

#### 2. Rule Services (services)

```yaml
services:
  build:
    context: .
    args:
      APP: WSFrontend/org.openl.rules.ruleservice.ws/target/webapp
  image: openltablets/ws
  ports:
    - "8081:8080"   # HTTP
    - "5006:5005"   # Debug
  volumes:
    - jars:/opt/openl/lib
  environment:
    PRODUCTION-REPOSITORY__REF_: repo-jdbc
    PRODUCTION-REPOSITORY_URI: jdbc:postgresql://postgres:5432/db?currentSchema=repository
    PRODUCTION-REPOSITORY_LOGIN: user
    PRODUCTION-REPOSITORY_PASSWORD: s3cr3t
    RULESERVICE_DEPLOYER_ENABLED: "true"
    ruleservice.datasource.deploy.classpath.jars: "true"
```

#### 3. PostgreSQL (postgres)

```yaml
postgres:
  image: postgres:alpine
  environment:
    POSTGRES_USER: user
    POSTGRES_PASSWORD: s3cr3t
    POSTGRES_DB: db
  ports:
    - "5432:5432"
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -d db -U user"]
    interval: 1s
    timeout: 5s
    retries: 10
```

#### 4. Nginx Proxy (proxy)

```yaml
proxy:
  image: nginx:alpine
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - www_cache:/var/cache/nginx
    - www_conf:/etc/nginx
```

### Volume Management

```yaml
volumes:
  jars:         # JDBC drivers and external JARs
  initdb:       # PostgreSQL initialization scripts
  www_conf:     # Nginx configuration
  www_cache:    # Nginx cache
  openl-home:   # OpenL shared workspace
```

### Commands

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# View specific service logs
docker compose logs -f studio

# Stop all services
docker compose down

# Stop and remove volumes (⚠️ deletes data)
docker compose down -v

# Restart service
docker compose restart studio

# Scale services (if configured)
docker compose up -d --scale services=3
```

---

## Building Docker Images

### Build from Source

```bash
# Build project
mvn clean package -DskipTests

# Build Web Studio image
docker build \
  --build-arg APP=STUDIO/org.openl.rules.webstudio/target/webapp \
  -t openltablets/webstudio:6.0.0 \
  .

# Build Rule Services image
docker build \
  --build-arg APP=WSFrontend/org.openl.rules.ruleservice.ws/target/webapp \
  -t openltablets/ws:6.0.0 \
  .
```

### Build with Docker Compose

```bash
# Build all images
docker compose build

# Build specific service
docker compose build studio

# Build without cache
docker compose build --no-cache

# Build with progress
docker compose build --progress=plain
```

### Multi-Platform Build

```bash
# Set up buildx
docker buildx create --use

# Build for multiple platforms
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t openltablets/webstudio:6.0.0 \
  --push \
  .
```

---

## Configuration

### Environment Variables

#### JVM Configuration

```bash
# Basic JVM settings
JAVA_OPTS="-Xms512m -Xmx2g"

# Use percentage of container memory (recommended)
JAVA_OPTS="-Xms32m -XX:MaxRAMPercentage=90.0"

# GC tuning
JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Debugging
JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005"
```

#### Jetty Configuration

```bash
# Automatically configured in setenv.sh:
-Dorg.eclipse.jetty.server.Request.maxFormContentSize=-1
-Dorg.eclipse.jetty.server.Request.maxFormKeys=-1
-Djetty.httpConfig.requestHeaderSize=32768
-Djetty.httpConfig.responseHeaderSize=32768
```

#### OpenTelemetry Configuration

```bash
# Enable OpenTelemetry
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318
OTEL_SERVICE_NAME=openl-studio

# Disable OpenTelemetry
OTEL_JAVAAGENT_ENABLED=false

# Disable OpenL rules tracing
OTEL_INSTRUMENTATION_OPENL_RULES_ENABLED=false
```

### Configuration Files

#### Application Properties

Mount custom application.properties:

```bash
docker run -d \
  -v /path/to/application.properties:/opt/openl/shared/application.properties \
  openltablets/webstudio
```

#### Custom Startup Configuration

Mount custom `setenv.sh`:

```bash
# Create setenv.sh
cat > setenv.sh <<'EOF'
export JAVA_OPTS="$JAVA_OPTS \
  -Xmx4g \
  -Dspring.profiles.active=production \
  -Dlogging.level.org.openl=DEBUG"
EOF

# Mount it
docker run -d \
  -v $(pwd)/setenv.sh:/opt/openl/setenv.sh \
  openltablets/webstudio
```

---

## Volumes and Persistence

### Persistent Volumes

#### Shared Workspace (Required)

```bash
# Named volume (recommended)
docker run -d \
  -v openl-shared:/opt/openl/shared \
  openltablets/webstudio

# Bind mount (for development)
docker run -d \
  -v /path/to/shared:/opt/openl/shared \
  openltablets/webstudio
```

**Contents**:
- `.properties`: Application configuration
- Projects, deployments, user data

#### External Libraries

```bash
# Mount JDBC drivers
docker run -d \
  -v /path/to/libs:/opt/openl/lib \
  openltablets/webstudio

# Example: Add PostgreSQL driver
mkdir libs
wget -O libs/postgresql.jar \
  https://jdbc.postgresql.org/download/postgresql-42.7.7.jar
```

#### Logs

```bash
# Mount logs directory
docker run -d \
  -v /path/to/logs:/opt/openl/logs \
  openltablets/webstudio
```

### Backup and Restore

#### Backup Volumes

```bash
# Backup named volume
docker run --rm \
  -v openl-shared:/data \
  -v $(pwd):/backup \
  alpine \
  tar czf /backup/openl-backup-$(date +%Y%m%d).tar.gz -C /data .

# Backup with docker compose
docker compose exec studio tar czf - /opt/openl/shared \
  > openl-backup.tar.gz
```

#### Restore Volumes

```bash
# Restore to named volume
docker run --rm \
  -v openl-shared:/data \
  -v $(pwd):/backup \
  alpine \
  tar xzf /backup/openl-backup-20250101.tar.gz -C /data

# Restore with docker compose
docker compose stop studio
docker compose exec studio bash -c "cd /opt/openl && tar xzf /backup/openl-backup.tar.gz"
docker compose start studio
```

---

## Networking

### Exposing Ports

```bash
# Expose on specific port
docker run -d -p 9090:8080 openltablets/webstudio

# Expose on specific interface
docker run -d -p 127.0.0.1:8080:8080 openltablets/webstudio

# Expose debug port
docker run -d \
  -p 8080:8080 \
  -p 5005:5005 \
  -e JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005" \
  openltablets/webstudio
```

### Custom Networks

```bash
# Create network
docker network create openl-network

# Run containers on network
docker run -d \
  --name openl-studio \
  --network openl-network \
  openltablets/webstudio

docker run -d \
  --name openl-services \
  --network openl-network \
  openltablets/ws
```

### Reverse Proxy Setup

#### Nginx

```nginx
# nginx.conf
server {
    listen 80;
    server_name openl.example.com;

    location /studio/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $http_host;
        proxy_set_header X-Forwarded-Prefix /studio;
        proxy_cookie_path / /studio/;
    }

    location /services/ {
        proxy_pass http://localhost:8081/;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

#### Traefik

```yaml
# docker-compose.yml with Traefik
services:
  studio:
    image: openltablets/webstudio
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.studio.rule=Host(`openl.example.com`) && PathPrefix(`/studio`)"
      - "traefik.http.routers.studio.middlewares=studio-stripprefix"
      - "traefik.http.middlewares.studio-stripprefix.stripprefix.prefixes=/studio"
```

---

## Security

### Running as Non-Root

OpenL Tablets Docker images run as user `openl` (UID 1000) by default:

```dockerfile
RUN adduser -S -D -s /usr/sbin/nologin -u 1000 openl
USER openl
```

### File Permissions

```bash
# Fix permissions for bind mounts
chown -R 1000:1000 /path/to/shared

# Or in Docker
docker run --rm \
  -v openl-shared:/data \
  alpine chown -R 1000:1000 /data
```

### Secrets Management

```bash
# Use Docker secrets (Swarm)
echo "s3cr3t" | docker secret create db_password -

# Use in service
docker service create \
  --name openl-studio \
  --secret db_password \
  openltablets/webstudio
```

### Environment Variables for Secrets

```bash
# Use environment variables
docker run -d \
  -e DB_PASSWORD_FILE=/run/secrets/db_password \
  openltablets/webstudio

# Or use .env file
cat > .env <<EOF
DB_PASSWORD=s3cr3t
ADMIN_PASSWORD=admin123
EOF

docker run -d --env-file .env openltablets/webstudio
```

### SSL/TLS Configuration

```bash
# Generate self-signed certificate
openssl req -x509 -newkey rsa:4096 \
  -keyout key.pem -out cert.pem \
  -days 365 -nodes

# Mount certificates
docker run -d \
  -v $(pwd)/key.pem:/etc/ssl/private/key.pem:ro \
  -v $(pwd)/cert.pem:/etc/ssl/certs/cert.pem:ro \
  openltablets/webstudio
```

---

## Performance Tuning

### Resource Limits

```bash
# Set memory and CPU limits
docker run -d \
  --memory="2g" \
  --memory-reservation="1g" \
  --cpus="2.0" \
  openltablets/webstudio

# With docker compose
services:
  studio:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
        reservations:
          memory: 1G
```

### JVM Tuning

```bash
# Optimize for container
docker run -d \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC" \
  openltablets/webstudio

# For large heaps (> 4GB)
docker run -d \
  --memory="8g" \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC" \
  openltablets/webstudio
```

### Health Checks

```yaml
# docker-compose.yml
services:
  studio:
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:8080/"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

---

## Monitoring

### Container Logs

```bash
# View logs
docker logs -f openl-studio

# View with timestamps
docker logs -t openl-studio

# Tail last 100 lines
docker logs --tail 100 openl-studio

# Since specific time
docker logs --since 2025-01-01T00:00:00 openl-studio
```

### Container Stats

```bash
# Real-time stats
docker stats openl-studio

# All containers
docker stats
```

### Health Checks

```bash
# Check container health
docker inspect --format='{{.State.Health.Status}}' openl-studio

# View health check logs
docker inspect --format='{{json .State.Health}}' openl-studio | jq
```

### OpenTelemetry Integration

```yaml
# docker-compose.yml with Jaeger
services:
  studio:
    environment:
      OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318
      OTEL_SERVICE_NAME: openl-studio

  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # Jaeger UI
      - "4318:4318"    # OTLP HTTP
```

---

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker logs openl-studio

# Check events
docker events --filter container=openl-studio

# Inspect container
docker inspect openl-studio
```

### Out of Memory

```bash
# Increase memory limit
docker update --memory="4g" openl-studio

# Or restart with new limit
docker stop openl-studio
docker rm openl-studio
docker run -d --memory="4g" --name openl-studio openltablets/webstudio
```

### Permission Issues

```bash
# Fix volume permissions
docker run --rm \
  -v openl-shared:/data \
  alpine chown -R 1000:1000 /data

# Or run as root temporarily
docker run --user root -it openltablets/webstudio sh
```

### Network Connectivity

```bash
# Test connectivity
docker exec openl-studio ping -c 3 postgres

# Check DNS
docker exec openl-studio nslookup postgres

# Check ports
docker exec openl-studio netstat -tulpn
```

### Debug Mode

```bash
# Start with debug
docker run -d \
  -p 8080:8080 \
  -p 5005:5005 \
  -e JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005" \
  openltablets/webstudio

# Connect debugger to localhost:5005
```

### Shell Access

```bash
# Start shell in running container
docker exec -it openl-studio sh

# Start shell in new container
docker run --rm -it openltablets/webstudio sh

# Run as root
docker exec -u root -it openl-studio sh
```

---

## Best Practices

### 1. Use Named Volumes

```bash
# ✅ Good
docker run -v openl-data:/opt/openl/shared openltablets/webstudio

# ❌ Bad (data lost on container removal)
docker run openltablets/webstudio
```

### 2. Set Resource Limits

```bash
docker run -d \
  --memory="2g" \
  --memory-reservation="1g" \
  --cpus="2.0" \
  openltablets/webstudio
```

### 3. Use Health Checks

```yaml
healthcheck:
  test: ["CMD", "wget", "--spider", "-q", "http://localhost:8080/"]
  interval: 30s
  timeout: 10s
  retries: 3
```

### 4. Implement Logging

```bash
# Use logging driver
docker run -d \
  --log-driver=json-file \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  openltablets/webstudio
```

### 5. Regular Backups

```bash
# Automated backup script
#!/bin/bash
docker run --rm \
  -v openl-shared:/data \
  -v /backup:/backup \
  alpine \
  tar czf /backup/openl-$(date +%Y%m%d).tar.gz -C /data .
```

---

## Related Documentation

- [CI/CD Pipeline](ci-cd.md) - Docker image builds
- [Performance Tuning](../guides/performance-tuning.md) - Performance optimization
- [Migration Guide](../guides/migration-guide.md) - Migration procedures
- [Troubleshooting](../onboarding/troubleshooting.md) - Common issues

---

**Last Updated**: 2025-11-05
**Maintainer**: OpenL Tablets Team

# Docker Deployment Files

This directory contains Docker Compose configurations and supporting files for deploying OpenL Tablets.

## Quick Start

```bash
# Simple single-container deployment (development)
docker-compose -f docker-compose-simple.yaml up -d

# Multi-container deployment (small production)
docker-compose -f docker-compose-multi.yaml up -d

# Full stack with HA (enterprise production)
docker-compose -f docker-compose-full.yaml up -d
```

## Files Overview

### Docker Compose Files

| File | Purpose | Components | Use Case |
|------|---------|------------|----------|
| **docker-compose-simple.yaml** | Single container | Studio + H2 | Development, testing |
| **docker-compose-multi.yaml** | Multi-container | Studio, Rules, PostgreSQL | Small production |
| **docker-compose-full.yaml** | Full stack | All + LB, Redis, Monitoring | Enterprise production |

### Configuration Files

| File | Purpose | Required For |
|------|---------|--------------|
| **nginx.conf** | Load balancer configuration | docker-compose-full.yaml |
| **init-db.sql** | Database initialization | docker-compose-multi.yaml, docker-compose-full.yaml |
| **prometheus.yml** | Monitoring configuration | docker-compose-full.yaml |

### Documentation

| File | Description |
|------|-------------|
| **index.md** | Comprehensive deployment guide |
| **quick-start.md** | Quick Docker deployment instructions |
| **README.md** | This file |

## Deployment Options

### Option 1: Simple (Development)

**File**: `docker-compose-simple.yaml`

Single OpenL Studio container with embedded H2 database.

```bash
docker-compose -f docker-compose-simple.yaml up -d
```

**Access**: http://localhost:8080

**Best for**:
- Local development
- Quick testing
- Learning OpenL Tablets

### Option 2: Multi-Container (Small Production)

**File**: `docker-compose-multi.yaml`

Separated OpenL Studio, Rule Services, and PostgreSQL.

**Prerequisites**:
- Copy `init-db.sql` to the same directory

```bash
docker-compose -f docker-compose-multi.yaml up -d
```

**Access**:
- Studio: http://localhost:8080
- Rule Services: http://localhost:9090

**Best for**:
- Small production deployments
- Team development
- Realistic testing

### Option 3: Full Stack (Production)

**File**: `docker-compose-full.yaml`

Complete production setup with load balancing, caching, and monitoring.

**Prerequisites**:
- Copy all configuration files to the same directory:
  - `nginx.conf`
  - `init-db.sql`
  - `prometheus.yml`
- Create `grafana` directory with dashboards and datasources

```bash
docker-compose -f docker-compose-full.yaml up -d
```

**Access**:
- Studio: http://localhost (via load balancer)
- Rule Services: http://localhost/ruleservices
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090

**Best for**:
- Production deployment
- High availability
- Performance testing

## Configuration

### Environment Variables

All docker-compose files support environment variable overrides.

Create `.env` file:

```bash
# Database
DATABASE_PASSWORD=your_secure_password

# Security
ADMIN_PASSWORD=your_admin_password

# Java Memory
STUDIO_MEMORY=2g
RULES_MEMORY=4g
```

### Custom Configuration

#### Studio Configuration

Create `config/studio/application.properties`:

```properties
# Database
spring.datasource.url=${DATABASE_URL}

# Security
security.mode=multi-user

# Repository
repository.type=git
```

Mount with:
```yaml
volumes:
  - ./config/studio/application.properties:/opt/openl/config/application.properties:ro
```

#### Rule Services Configuration

Create `config/ruleservices/application.properties`:

```properties
# API
ruleservice.api.enabled=true
ruleservice.openapi.enabled=true

# Caching
ruleservice.cache.enabled=true
```

Mount with:
```yaml
volumes:
  - ./config/ruleservices/application.properties:/opt/openl/config/application.properties:ro
```

## Monitoring Setup

### Grafana Dashboards

Create `grafana/dashboards/dashboard.yml`:

```yaml
apiVersion: 1

providers:
  - name: 'OpenL Tablets'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    options:
      path: /etc/grafana/provisioning/dashboards
```

Create `grafana/datasources/prometheus.yml`:

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

### Prometheus Alerts (Optional)

Create `alert_rules.yml`:

```yaml
groups:
  - name: openl_alerts
    interval: 30s
    rules:
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage on {{ $labels.instance }}"

      - alert: ServiceDown
        expr: up == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} down"
```

## Security

### Production Checklist

Before deploying to production:

1. **Change all passwords**:
   ```bash
   # In docker-compose files, change:
   ADMIN_PASSWORD=admin              # ❌ Change this
   POSTGRES_PASSWORD=changeme        # ❌ Change this
   SPRING_REDIS_PASSWORD=redis_password  # ❌ Change this
   ```

2. **Enable HTTPS**:
   - Generate SSL certificates
   - Uncomment HTTPS section in `nginx.conf`
   - Mount certificates: `./certs:/etc/nginx/certs:ro`

3. **Use Docker secrets** (Docker Swarm/Kubernetes):
   ```yaml
   secrets:
     - db_password
   environment:
     DATABASE_PASSWORD_FILE: /run/secrets/db_password
   ```

4. **Restrict network access**:
   - Set `internal: true` for backend networks
   - Remove unnecessary port exposures
   - Use firewall rules

5. **Enable monitoring**:
   - Configure Prometheus alerts
   - Set up log aggregation
   - Monitor resource usage

## Backup and Restore

### Database Backup

```bash
# Backup
docker exec openl-postgres pg_dump -U openl openl_studio > backup-studio.sql
docker exec openl-postgres pg_dump -U openl openl_ruleservices > backup-rules.sql

# Restore
docker exec -i openl-postgres psql -U openl openl_studio < backup-studio.sql
docker exec -i openl-postgres psql -U openl openl_ruleservices < backup-rules.sql
```

### Workspace Backup

```bash
# Backup
docker run --rm \
  -v openl-studio-workspace:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/workspace-backup.tar.gz -C /data .

# Restore
docker run --rm \
  -v openl-studio-workspace:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/workspace-backup.tar.gz -C /data
```

## Troubleshooting

### Container Fails to Start

```bash
# View logs
docker logs openl-studio

# Check status
docker ps -a

# Restart container
docker-compose -f docker-compose-*.yaml restart studio
```

### Database Connection Issues

```bash
# Test connectivity
docker exec openl-studio nc -zv postgres 5432

# Check database logs
docker logs openl-postgres

# Verify credentials
docker exec openl-postgres psql -U openl -l
```

### Port Conflicts

```bash
# Find process using port
lsof -i :8080

# Change port mapping in docker-compose file
ports:
  - "8081:8080"  # Use host port 8081 instead
```

### Volume Permission Issues

```bash
# Fix ownership (Linux)
sudo chown -R 1000:1000 ./data

# Or run as specific user
user: "1000:1000"  # Add to service definition
```

## Upgrading

### Minor Version

```bash
# Stop services
docker-compose -f docker-compose-*.yaml down

# Backup data
./backup.sh

# Pull new images
docker-compose -f docker-compose-*.yaml pull

# Start with new images
docker-compose -f docker-compose-*.yaml up -d
```

### Major Version

Follow [Upgrade Guide](../../operations/upgrade.md) for major version upgrades.

## Related Documentation

- [Docker Deployment Guide](index.md) - Comprehensive guide
- [Deployment Overview](../) - All deployment options
- [Configuration Reference](../../configuration/) - Configuration options
- [Security Guide](../../configuration/security.md) - Security best practices

## Support

- **Issues**: [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)
- **Discussions**: [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)
- **Documentation**: [OpenL Tablets Docs](https://openl-tablets.readthedocs.io)

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

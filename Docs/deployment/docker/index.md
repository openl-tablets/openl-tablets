# Docker Deployment

Comprehensive guide for deploying OpenL Tablets using Docker and Docker Compose.

## Overview

Docker provides containerized deployment for OpenL Tablets, offering:

- **Fast setup**: Get running in minutes
- **Consistency**: Same environment across dev, test, and production
- **Isolation**: Each component runs in its own container
- **Portability**: Deploy anywhere Docker runs
- **Scalability**: Easy horizontal scaling with orchestration

## Deployment Patterns

### Pattern 1: Simple Single Container

**Use Case**: Development, testing, quick evaluation

```
┌─────────────────────────────────┐
│  Docker Container               │
│  ┌───────────────────────────┐ │
│  │  OpenL Studio             │ │
│  │  + Embedded H2 Database   │ │
│  └───────────────────────────┘ │
└─────────────────────────────────┘
```

- Single container with embedded database
- No external dependencies
- Data persistence through volumes
- **Best for**: Quick start, development, demos

### Pattern 2: Multi-Container Separated

**Use Case**: Small production, realistic testing

```
┌──────────────────┐  ┌──────────────────┐  ┌─────────────┐
│  OpenL Studio    │  │  Rule Services   │  │  PostgreSQL │
│  (Port 8080)     │  │  (Port 9090)     │  │  (Port 5432)│
└──────────────────┘  └──────────────────┘  └─────────────┘
```

- Separated Studio and Rule Services
- External PostgreSQL database
- Independent scaling
- **Best for**: Small production, team development

### Pattern 3: Production with HA

**Use Case**: Enterprise production, high availability

```
                        ┌──────────────┐
                        │ Nginx / LB   │
                        └──────┬───────┘
                ┌──────────────┼──────────────┐
         ┌──────▼──────┐ ┌────▼──────┐ ┌────▼──────┐
         │   Studio 1  │ │  Studio 2 │ │  Studio 3 │
         └─────────────┘ └───────────┘ └───────────┘
                        ┌──────────────┐
                        │    Redis     │
                        └──────┬───────┘
                ┌──────────────┼──────────────┐
         ┌──────▼──────┐ ┌────▼──────┐ ┌────▼──────┐
         │   Rules 1   │ │  Rules 2  │ │  Rules 3  │
         └─────────────┘ └───────────┘ └───────────┘
                        ┌──────────────┐
                        │  PostgreSQL  │
                        │   (Primary)  │
                        └──────┬───────┘
                        ┌──────▼───────┐
                        │  PostgreSQL  │
                        │   (Replica)  │
                        └──────────────┘
```

- Multiple Studio and Rule Services instances
- Load balancing
- Redis for session management
- PostgreSQL replication
- Monitoring with Prometheus/Grafana
- **Best for**: Production, high availability

---

## Quick Start

### Prerequisites

- Docker 24.0+ installed
- Docker Compose 2.0+ installed
- 4GB+ RAM available
- 10GB+ disk space

### Simple Deployment (5 Minutes)

Run OpenL Studio with embedded database:

```bash
docker run -d \
  --name openl-studio \
  -p 8080:8080 \
  -v openl-workspace:/opt/openl/workspace \
  openltablets/studio:latest

# Access OpenL Studio
open http://localhost:8080
```

**Initial Credentials**:
- Username: `admin`
- Password: `admin`

⚠️ **Change password immediately after first login!**

### Docker Compose Deployment

For more complex setups, use Docker Compose:

```bash
# Download compose file
curl -O https://raw.githubusercontent.com/openl-tablets/openl-tablets/master/docs/deployment/docker/docker-compose-simple.yaml

# Start services
docker-compose -f docker-compose-simple.yaml up -d

# View logs
docker-compose -f docker-compose-simple.yaml logs -f

# Stop services
docker-compose -f docker-compose-simple.yaml down
```

---

## Deployment Options

### Option 1: Simple (Development)

**File**: [`docker-compose-simple.yaml`](docker-compose-simple.yaml)

**Components**:
- OpenL Studio with embedded H2 database
- Single container

**Use Cases**:
- Local development
- Quick testing
- Learning OpenL Tablets

**Start**:
```bash
docker-compose -f docker-compose-simple.yaml up -d
```

**Access**:
- Studio: http://localhost:8080

### Option 2: Multi-Container (Small Production)

**File**: [`docker-compose-multi.yaml`](docker-compose-multi.yaml)

**Components**:
- OpenL Studio (port 8080)
- OpenL Rule Services (port 9090)
- PostgreSQL database (port 5432)

**Use Cases**:
- Small production deployments
- Team development
- Realistic testing

**Start**:
```bash
docker-compose -f docker-compose-multi.yaml up -d
```

**Access**:
- Studio: http://localhost:8080
- Rule Services: http://localhost:9090

### Option 3: Full Stack (Production)

**File**: [`docker-compose-full.yaml`](docker-compose-full.yaml)

**Components**:
- OpenL Studio (3 replicas, ports 8081-8083)
- OpenL Rule Services (3 replicas, ports 9091-9093)
- PostgreSQL (primary + replica)
- Redis (session management)
- Nginx (load balancer)
- Prometheus (metrics)
- Grafana (monitoring)

**Use Cases**:
- Production deployment
- High availability
- Performance testing
- Complete monitoring

**Start**:
```bash
docker-compose -f docker-compose-full.yaml up -d
```

**Access**:
- Studio (via LB): http://localhost:80
- Rule Services (via LB): http://localhost:80/ruleservices
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090

---

## Configuration

### Environment Variables

#### OpenL Studio

```bash
# Database Configuration
DATABASE_URL=postgresql://user:password@postgres:5432/openl
DATABASE_DRIVER=org.postgresql.Driver

# Security
SECURITY_MODE=multi-user  # or single-user, ad
ADMIN_USERNAME=admin
ADMIN_PASSWORD=changeme

# Workspace
STUDIO_WORKSPACE=/opt/openl/workspace

# Java Options
JAVA_OPTS=-Xmx2g -Xms512m

# Logging
LOGGING_LEVEL=INFO
```

#### OpenL Rule Services

```bash
# Database Configuration
DATABASE_URL=postgresql://user:password@postgres:5432/openl

# Deployment Configuration
RULESERVICE_DEPLOYMENT_PATH=/opt/openl/deployments
RULESERVICE_DATASOURCE_ENABLED=true

# Java Options
JAVA_OPTS=-Xmx4g -Xms1g

# API Configuration
RULESERVICE_API_ENABLED=true
RULESERVICE_OPENAPI_ENABLED=true
```

### Volume Mounts

#### OpenL Studio Volumes

```yaml
volumes:
  # Workspace (projects, settings)
  - ./data/workspace:/opt/openl/workspace

  # Logs
  - ./logs/studio:/opt/openl/logs

  # Configuration overrides
  - ./config/studio:/opt/openl/config
```

#### Rule Services Volumes

```yaml
volumes:
  # Deployments (rule artifacts)
  - ./data/deployments:/opt/openl/deployments

  # Logs
  - ./logs/ruleservices:/opt/openl/logs

  # Configuration overrides
  - ./config/ruleservices:/opt/openl/config
```

### Custom Configuration Files

#### application.properties

Create `config/studio/application.properties`:

```properties
# Database
spring.datasource.url=${DATABASE_URL}
spring.datasource.driver-class-name=${DATABASE_DRIVER}

# Security
security.mode=${SECURITY_MODE:multi-user}

# Repository
repository.type=git
repository.path=/opt/openl/workspace/repositories

# User workspace
user.workspace.home=/opt/openl/workspace/user-workspaces

# Logging
logging.level.org.openl=${LOGGING_LEVEL:INFO}
```

Mount with:
```yaml
volumes:
  - ./config/studio/application.properties:/opt/openl/config/application.properties:ro
```

---

## Database Configuration

### PostgreSQL (Recommended)

```yaml
postgres:
  image: postgres:16-alpine
  environment:
    POSTGRES_DB: openl
    POSTGRES_USER: openl
    POSTGRES_PASSWORD: changeme
  volumes:
    - postgres-data:/var/lib/postgresql/data
  ports:
    - "5432:5432"
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U openl"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### Database Initialization

Create `init-db.sql`:

```sql
-- Create databases
CREATE DATABASE openl_studio;
CREATE DATABASE openl_ruleservices;

-- Create users
CREATE USER studio_user WITH PASSWORD 'studio_password';
CREATE USER rules_user WITH PASSWORD 'rules_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE openl_studio TO studio_user;
GRANT ALL PRIVILEGES ON DATABASE openl_ruleservices TO rules_user;
```

Mount with:
```yaml
volumes:
  - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql:ro
```

### MySQL/MariaDB (Alternative)

```yaml
mysql:
  image: mysql:8.0
  environment:
    MYSQL_ROOT_PASSWORD: rootpassword
    MYSQL_DATABASE: openl
    MYSQL_USER: openl
    MYSQL_PASSWORD: changeme
  volumes:
    - mysql-data:/var/lib/mysql
  ports:
    - "3306:3306"
  command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

---

## Networking

### Default Bridge Network

Simple deployments use Docker's default bridge network:

```yaml
networks:
  default:
    driver: bridge
```

### Custom Network

For better isolation and control:

```yaml
networks:
  openl-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.25.0.0/16

services:
  studio:
    networks:
      - openl-network

  postgres:
    networks:
      - openl-network
```

### Multi-Network Setup

Separate frontend and backend networks:

```yaml
networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge

services:
  nginx:
    networks:
      - frontend

  studio:
    networks:
      - frontend
      - backend

  postgres:
    networks:
      - backend  # Not exposed to frontend
```

---

## Load Balancing

### Nginx Configuration

Create `nginx.conf`:

```nginx
upstream studio_backend {
    least_conn;
    server studio-1:8080 max_fails=3 fail_timeout=30s;
    server studio-2:8080 max_fails=3 fail_timeout=30s;
    server studio-3:8080 max_fails=3 fail_timeout=30s;
}

upstream rules_backend {
    least_conn;
    server ruleservices-1:8080 max_fails=3 fail_timeout=30s;
    server ruleservices-2:8080 max_fails=3 fail_timeout=30s;
    server ruleservices-3:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name _;

    location / {
        proxy_pass http://studio_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /ruleservices/ {
        proxy_pass http://rules_backend/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Deploy with Docker Compose:

```yaml
nginx:
  image: nginx:alpine
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - ./nginx.conf:/etc/nginx/nginx.conf:ro
  depends_on:
    - studio-1
    - studio-2
    - studio-3
    - ruleservices-1
    - ruleservices-2
    - ruleservices-3
```

---

## Security

### HTTPS/TLS Configuration

#### Generate Self-Signed Certificate (Development)

```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout ./certs/nginx-selfsigned.key \
  -out ./certs/nginx-selfsigned.crt \
  -subj "/C=US/ST=State/L=City/O=Organization/CN=localhost"
```

#### Nginx HTTPS Configuration

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/nginx/certs/nginx-selfsigned.crt;
    ssl_certificate_key /etc/nginx/certs/nginx-selfsigned.key;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    location / {
        proxy_pass http://studio_backend;
        # ... proxy headers
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}
```

### Secrets Management

Use Docker secrets instead of environment variables:

```yaml
services:
  studio:
    secrets:
      - db_password
      - admin_password
    environment:
      DATABASE_PASSWORD_FILE: /run/secrets/db_password
      ADMIN_PASSWORD_FILE: /run/secrets/admin_password

secrets:
  db_password:
    file: ./secrets/db_password.txt
  admin_password:
    file: ./secrets/admin_password.txt
```

### Network Isolation

```yaml
services:
  # Public-facing
  nginx:
    networks:
      - public
      - internal

  # Internal only
  studio:
    networks:
      - internal

  postgres:
    networks:
      - internal
    # No port exposure to host

networks:
  public:
    driver: bridge
  internal:
    driver: bridge
    internal: true  # No external connectivity
```

---

## Monitoring

### Health Checks

All services should have health checks:

```yaml
studio:
  image: openltablets/studio
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 60s
```

### Prometheus Metrics

OpenL Tablets exposes Prometheus metrics via Spring Actuator:

```yaml
prometheus:
  image: prom/prometheus:latest
  ports:
    - "9090:9090"
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
```

Create `prometheus.yml`:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'openl-studio'
    static_configs:
      - targets: ['studio-1:8080', 'studio-2:8080', 'studio-3:8080']
    metrics_path: '/actuator/prometheus'

  - job_name: 'openl-ruleservices'
    static_configs:
      - targets: ['ruleservices-1:8080', 'ruleservices-2:8080', 'ruleservices-3:8080']
    metrics_path: '/actuator/prometheus'
```

### Grafana Dashboards

```yaml
grafana:
  image: grafana/grafana:latest
  ports:
    - "3000:3000"
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
    - GF_USERS_ALLOW_SIGN_UP=false
  volumes:
    - grafana-data:/var/lib/grafana
    - ./grafana/dashboards:/etc/grafana/provisioning/dashboards:ro
    - ./grafana/datasources:/etc/grafana/provisioning/datasources:ro
```

---

## Backup and Restore

### Database Backup

```bash
# Backup PostgreSQL
docker exec openl-postgres pg_dump -U openl openl > backup-$(date +%Y%m%d-%H%M%S).sql

# Restore PostgreSQL
docker exec -i openl-postgres psql -U openl openl < backup-20250101-120000.sql
```

### Workspace Backup

```bash
# Backup workspace
docker run --rm \
  -v openl-workspace:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/workspace-backup-$(date +%Y%m%d).tar.gz -C /data .

# Restore workspace
docker run --rm \
  -v openl-workspace:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/workspace-backup-20250101.tar.gz -C /data
```

### Automated Backups

Create backup script `backup.sh`:

```bash
#!/bin/bash
BACKUP_DIR=/backups
DATE=$(date +%Y%m%d-%H%M%S)

# Backup database
docker exec openl-postgres pg_dump -U openl openl | gzip > $BACKUP_DIR/db-$DATE.sql.gz

# Backup workspace
docker run --rm \
  -v openl-workspace:/data \
  -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/workspace-$DATE.tar.gz -C /data .

# Keep only last 7 days
find $BACKUP_DIR -name "db-*.sql.gz" -mtime +7 -delete
find $BACKUP_DIR -name "workspace-*.tar.gz" -mtime +7 -delete
```

Schedule with cron:
```cron
0 2 * * * /path/to/backup.sh
```

---

## Troubleshooting

### Container Fails to Start

```bash
# View logs
docker logs openl-studio

# Check container status
docker ps -a

# Inspect container
docker inspect openl-studio

# Common issues:
# - Port already in use: Change port mapping
# - Volume permissions: Check ownership
# - Memory limits: Increase Docker memory
```

### Database Connection Issues

```bash
# Test database connectivity
docker exec openl-studio nc -zv postgres 5432

# Check database logs
docker logs openl-postgres

# Verify credentials
docker exec openl-postgres psql -U openl -c "\l"

# Common issues:
# - Wrong DATABASE_URL format
# - Network isolation
# - Database not ready (add depends_on with health check)
```

### Performance Issues

```bash
# Check resource usage
docker stats

# View Java heap usage
docker exec openl-studio jstat -gc 1

# Increase memory
# Edit docker-compose.yaml:
environment:
  JAVA_OPTS: -Xmx4g -Xms1g

# Or for entire container:
deploy:
  resources:
    limits:
      memory: 4G
```

### Port Conflicts

```bash
# Find process using port
lsof -i :8080

# Change port mapping
docker run -p 8081:8080 openltablets/studio
```

### Volume Permission Issues

```bash
# Fix ownership (Linux)
sudo chown -R 1000:1000 ./data/workspace

# Or run container as specific user
docker run --user 1000:1000 -p 8080:8080 openltablets/studio
```

### Network Issues

```bash
# Inspect network
docker network inspect openl-network

# Test connectivity between containers
docker exec openl-studio ping postgres

# Recreate network
docker-compose down
docker network prune
docker-compose up -d
```

---

## Upgrading

### Minor Version Upgrade

```bash
# Stop services
docker-compose down

# Pull new images
docker-compose pull

# Backup data (important!)
./backup.sh

# Start with new images
docker-compose up -d

# Check logs
docker-compose logs -f
```

### Major Version Upgrade

For major version upgrades, follow these steps:

1. **Backup everything**:
   ```bash
   ./backup.sh
   docker-compose down
   ```

2. **Read release notes**: Check for breaking changes

3. **Test in staging**: Deploy to test environment first

4. **Update database schema** (if needed):
   ```bash
   docker exec openl-postgres psql -U openl openl < migration-script.sql
   ```

5. **Update configuration**: Review config changes

6. **Deploy**:
   ```bash
   docker-compose pull
   docker-compose up -d
   ```

7. **Verify**: Test all functionality

---

## Best Practices

### Production Checklist

✅ **Do**:
- Use external PostgreSQL (not H2)
- Enable HTTPS/TLS
- Use Docker secrets for passwords
- Set up automated backups
- Configure monitoring (Prometheus/Grafana)
- Use health checks
- Set resource limits
- Use specific image tags (not `latest`)
- Enable logging to external system
- Test disaster recovery

❌ **Don't**:
- Use embedded H2 in production
- Expose database ports externally
- Use default passwords
- Run containers as root
- Use `latest` tag in production
- Skip backups
- Ignore health check failures

### Resource Sizing

#### OpenL Studio

| Workload | CPU | Memory | Disk |
|----------|-----|--------|------|
| Small (< 10 users) | 2 cores | 2GB | 20GB |
| Medium (10-50 users) | 4 cores | 4GB | 50GB |
| Large (50+ users) | 8 cores | 8GB | 100GB |

#### OpenL Rule Services

| Workload | CPU | Memory | Disk |
|----------|-----|--------|------|
| Small (< 100 req/s) | 2 cores | 4GB | 10GB |
| Medium (100-1000 req/s) | 4 cores | 8GB | 20GB |
| Large (> 1000 req/s) | 8+ cores | 16GB+ | 50GB |

### Configuration Management

Use environment-specific compose files:

```bash
# Base configuration
docker-compose.yaml

# Environment overrides
docker-compose.dev.yaml
docker-compose.staging.yaml
docker-compose.prod.yaml

# Deploy to production
docker-compose -f docker-compose.yaml -f docker-compose.prod.yaml up -d
```

---

## Related Documentation

- [Deployment Overview](../) - All deployment options
- [Kubernetes Deployment](../kubernetes/) - Container orchestration
- [Cloud Deployment](../cloud/) - AWS, Azure, GCP
- [Configuration Reference](../../configuration/) - All configuration options
- [Security Guide](../../configuration/security.md) - Security best practices
- [Operations Guide](../../operations/docker-guide.md) - Advanced Docker usage

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

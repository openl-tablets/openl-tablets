# OpenL Tablets - Production Deployment Guide

**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-06

---

## Table of Contents

- [Overview](#overview)
- [Deployment Models](#deployment-models)
- [Prerequisites](#prerequisites)
- [Quick Start Deployments](#quick-start-deployments)
- [Docker Deployment](#docker-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Cloud Platform Deployments](#cloud-platform-deployments)
- [Traditional Application Server](#traditional-application-server)
- [Database Configuration](#database-configuration)
- [Security Configuration](#security-configuration)
- [Performance Tuning](#performance-tuning)
- [Monitoring and Observability](#monitoring-and-observability)
- [High Availability](#high-availability)
- [Backup and Disaster Recovery](#backup-and-disaster-recovery)
- [Troubleshooting](#troubleshooting)

---

## Overview

This guide covers production deployment of OpenL Tablets components:

- **OpenL Studio**: Web-based IDE for rule development
- **Rule Services**: REST services for rule execution
- **Both**: Combined deployment (dev + production)

### Deployment Architecture

```
┌────────────────────────────────────────────────────────────┐
│                    Load Balancer / Ingress                 │
│                 (HTTPS, SSL/TLS Termination)               │
└─────────────────────┬──────────────────────────────────────┘
                      │
       ┌──────────────┼──────────────┐
       │              │              │
┌──────▼──────┐ ┌─────▼──────┐ ┌─────▼──────┐
│  Rule       │ │  Rule      │ │  OpenL     │
│  Service 1  │ │  Service 2 │ │  Studio    │
└──────┬──────┘ └─────┬──────┘ └─────┬──────┘
       │              │              │
       └──────────────┼──────────────┘
                      │
       ┌──────────────┼───────────────┐
       │              │               │
┌──────▼──────────┐ ┌─▼────────────┐ ┌▼───────────┐
│   Database      │ │  Git Repo    │ │  File      │
│  (PostgreSQL)   │ │  (Rules)     │ │  Storage   │
└─────────────────┘ └──────────────┘ └────────────┘
```

---

## Deployment Models

### 1. Standalone Rule Services (Recommended for Production)

**Use Case**: Production rule execution
**Components**: WSFrontend only
**Characteristics**:
- Stateless (horizontally scalable)
- REST APIs
- Hot reload capability
- No UI (headless)

### 2. OpenL Studio (Development/Staging)

**Use Case**: Rule development and testing
**Components**: OpenL Studio only
**Characteristics**:
- Web-based IDE
- Git integration
- Testing tools
- User management

### 3. Combined Deployment (All-in-One)

**Use Case**: Small deployments, demos
**Components**: Studio + Rule Services
**Characteristics**:
- Single deployment
- Shared database
- Suitable for development/staging

### 4. Microservices (Enterprise)

**Use Case**: Large-scale enterprise
**Components**: Multiple rule services (by domain)
**Characteristics**:
- Domain-driven design
- Independent scaling
- Service mesh integration

---

## Prerequisites

### System Requirements

| Component | Minimum | Recommended | Notes |
|-----------|---------|-------------|-------|
| **CPU** | 2 cores | 4+ cores | More cores = faster compilation |
| **Memory** | 2 GB RAM | 4-8 GB RAM | Depends on rule complexity |
| **Disk** | 5 GB | 20+ GB | For rules, database, logs |
| **OS** | Linux, Windows, macOS | Linux (Ubuntu 22.04, RHEL 8+) | Production: Linux |

### Software Requirements

| Software | Version | Required For |
|----------|---------|--------------|
| **JDK** | 21+ | All components |
| **Database** | PostgreSQL 12+, MySQL 8.0+, MariaDB 10.5+ | OpenL Studio, Rule Services |
| **Git** | 2.30+ | Rule version control (optional) |
| **Docker** | 27.0+ | Container deployments |
| **Kubernetes** | 1.28+ | Orchestrated deployments |

### Network Requirements

| Component | Default Port | Protocol | Purpose |
|-----------|-------------|----------|---------|
| **OpenL Studio** | 8080 | HTTP/HTTPS | Web UI access |
| **Rule Service** | 8080 | HTTP/HTTPS | REST APIs |
| **Database** | 5432 (PostgreSQL) | TCP | Data storage |
| **Health Check** | 8080/actuator/health | HTTP | Liveness/readiness |

---

## Quick Start Deployments

### Docker Compose (Fastest)

**Use Case**: Quick evaluation, development

```bash
# Clone repository
git clone https://github.com/openl-tablets/openl-tablets.git
cd openl-tablets

# Start all services
docker compose up -d

# Access services
# OpenL Studio: http://localhost:8080
# Rule Service: http://localhost:8081
# Database: PostgreSQL on localhost:5432
```

**Services Included**:
- OpenL Studio
- Rule Service
- PostgreSQL database
- pgAdmin (database UI)

### Pre-built Docker Images

**Use Case**: Quick production deployment

```bash
# Pull latest images
docker pull openltablets/openl-studio:latest
docker pull openltablets/ws:latest

# Run OpenL Studio
docker run -d \
  --name openl-studio \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/openl \
  -e SPRING_DATASOURCE_USERNAME=openl \
  -e SPRING_DATASOURCE_PASSWORD=openl \
  openltablets/openl-studio:latest

# Run Rule Service
docker run -d \
  --name rule-service \
  -p 8081:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/openl \
  -e SPRING_DATASOURCE_USERNAME=openl \
  -e SPRING_DATASOURCE_PASSWORD=openl \
  openltablets/ws:latest
```

---

## Docker Deployment

### Building Custom Images

#### 1. Build OpenL Studio Image

```dockerfile
# Dockerfile for OpenL Studio
FROM eclipse-temurin:21-jre-alpine

# Create app directory
RUN mkdir -p /app
WORKDIR /app

# Copy WAR file
COPY STUDIO/org.openl.rules.webstudio/target/webapp.war /app/studio.war

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "/app/studio.war"]
```

Build:
```bash
# Build OpenL Tablets first
mvn clean install -DskipTests

# Build Docker image
docker build -f Dockerfile.studio -t my-openl-studio:6.0.0 .
```

#### 2. Build Rule Service Image

```dockerfile
# Dockerfile for Rule Service
FROM eclipse-temurin:21-jre-alpine

# Create app directory
RUN mkdir -p /app
WORKDIR /app

# Copy WAR file
COPY WSFrontend/org.openl.rules.ruleservice.ws/target/webapp.war /app/rule-service.war

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
  "-Xms512m", \
  "-Xmx2g", \
  "-XX:+UseG1GC", \
  "-jar", \
  "/app/rule-service.war"]
```

Build:
```bash
docker build -f Dockerfile.ruleservice -t my-rule-service:6.0.0 .
```

### Docker Compose Production Setup

**File**: `docker-compose.prod.yml`

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:16-alpine
    container_name: openl-postgres
    restart: always
    environment:
      POSTGRES_DB: openl_prod
      POSTGRES_USER: openl
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U openl"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - openl-network

  # OpenL Studio
  openl-studio:
    image: openltablets/openl-studio:6.0.0
    container_name: openl-studio
    restart: always
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      # Database
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/openl_prod
      SPRING_DATASOURCE_USERNAME: openl
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}

      # Application
      SERVER_PORT: 8080
      SPRING_PROFILES_ACTIVE: production

      # Security
      SECURITY_OAUTH2_ENABLED: ${OAUTH2_ENABLED:-false}
      SECURITY_SAML_ENABLED: ${SAML_ENABLED:-false}

      # Performance
      JAVA_OPTS: "-Xms1g -Xmx2g -XX:+UseG1GC"
    ports:
      - "8080:8080"
    volumes:
      - studio-data:/opt/openl/data
      - studio-logs:/opt/openl/logs
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - openl-network

  # Rule Service (Multiple instances for HA)
  rule-service-1:
    image: openltablets/ws:6.0.0
    container_name: rule-service-1
    restart: always
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/openl_prod
      SPRING_DATASOURCE_USERNAME: openl
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SERVER_PORT: 8080
      SPRING_PROFILES_ACTIVE: production
      JAVA_OPTS: "-Xms512m -Xmx2g -XX:+UseG1GC"
    ports:
      - "8081:8080"
    volumes:
      - rule-service-data:/opt/openl/data
      - rule-service-logs:/opt/openl/logs
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - openl-network

  rule-service-2:
    image: openltablets/ws:6.0.0
    container_name: rule-service-2
    restart: always
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/openl_prod
      SPRING_DATASOURCE_USERNAME: openl
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SERVER_PORT: 8080
      SPRING_PROFILES_ACTIVE: production
      JAVA_OPTS: "-Xms512m -Xmx2g -XX:+UseG1GC"
    ports:
      - "8082:8080"
    volumes:
      - rule-service-data:/opt/openl/data
      - rule-service-logs:/opt/openl/logs
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - openl-network

  # Nginx Load Balancer
  nginx:
    image: nginx:alpine
    container_name: openl-lb
    restart: always
    depends_on:
      - rule-service-1
      - rule-service-2
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    networks:
      - openl-network

volumes:
  postgres-data:
  studio-data:
  studio-logs:
  rule-service-data:
  rule-service-logs:

networks:
  openl-network:
    driver: bridge
```

**Nginx Configuration** (`nginx.conf`):

```nginx
upstream rule-services {
    least_conn;
    server rule-service-1:8080 max_fails=3 fail_timeout=30s;
    server rule-service-2:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    listen [::]:80;
    server_name your-domain.com;

    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name your-domain.com;

    # SSL Configuration
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # OpenL Studio
    location /studio/ {
        proxy_pass http://openl-studio:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Rule Services (Load Balanced)
    location /rules/ {
        proxy_pass http://rule-services/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Health check endpoint
    location /health {
        access_log off;
        return 200 "OK\n";
        add_header Content-Type text/plain;
    }
}
```

**Deploy**:
```bash
# Create .env file
cat > .env <<EOF
DB_PASSWORD=your-secure-password
OAUTH2_ENABLED=false
SAML_ENABLED=false
EOF

# Start services
docker compose -f docker-compose.prod.yml up -d

# View logs
docker compose -f docker-compose.prod.yml logs -f

# Scale rule services
docker compose -f docker-compose.prod.yml up -d --scale rule-service=4
```

---

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (1.28+)
- kubectl configured
- Helm 3.x (optional)
- Container registry access

### Architecture

```
┌────────────────────────────────────────────────────────────┐
│                  Ingress Controller                         │
│            (NGINX, Traefik, or Cloud LB)                    │
└────────────────────┬───────────────────────────────────────┘
                     │
     ┌───────────────┼───────────────┐
     │               │               │
┌────▼─────┐  ┌─────▼──────┐  ┌────▼──────┐
│ Studio   │  │ Rule       │  │ Rule      │
│ Service  │  │ Service    │  │ Service   │
│          │  │ Pod 1      │  │ Pod 2     │
└────┬─────┘  └─────┬──────┘  └────┬──────┘
     │              │              │
     └──────────────┼──────────────┘
                    │
         ┌──────────┼──────────┐
         │                     │
    ┌────▼────┐         ┌─────▼──────┐
    │ Database│         │ Persistent │
    │ Service │         │ Volume     │
    └─────────┘         └────────────┘
```

### 1. Namespace and ConfigMap

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: openl-tablets
  labels:
    name: openl-tablets
---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: openl-config
  namespace: openl-tablets
data:
  application.properties: |
    # Database
    spring.datasource.url=jdbc:postgresql://postgres-service:5432/openl_prod
    spring.datasource.username=openl
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

    # Application
    server.port=8080
    spring.profiles.active=production

    # Logging
    logging.level.org.openl=INFO
    logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### 2. Secrets

```yaml
# secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: openl-secrets
  namespace: openl-tablets
type: Opaque
stringData:
  db-password: "your-secure-password"
  oauth2-client-secret: "your-oauth2-secret"
  saml-keystore-password: "your-saml-password"
```

Apply secrets:
```bash
kubectl apply -f secrets.yaml
```

### 3. PostgreSQL Deployment

```yaml
# postgres-deployment.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: openl-tablets
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  storageClassName: standard
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: openl-tablets
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:16-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: "openl_prod"
        - name: POSTGRES_USER
          value: "openl"
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: openl-secrets
              key: db-password
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - openl
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - openl
          initialDelaySeconds: 5
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: openl-tablets
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  clusterIP: None
```

### 4. Rule Service Deployment

```yaml
# rule-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rule-service
  namespace: openl-tablets
  labels:
    app: rule-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: rule-service
  template:
    metadata:
      labels:
        app: rule-service
    spec:
      containers:
      - name: rule-service
        image: openltablets/ws:6.0.0
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres-service:5432/openl_prod"
        - name: SPRING_DATASOURCE_USERNAME
          value: "openl"
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: openl-secrets
              key: db-password
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: JAVA_OPTS
          value: "-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
        volumeMounts:
        - name: rule-data
          mountPath: /opt/openl/data
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "3Gi"
            cpu: "2000m"
      volumes:
      - name: rule-data
        emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: rule-service
  namespace: openl-tablets
spec:
  type: ClusterIP
  selector:
    app: rule-service
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
    name: http
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: rule-service-hpa
  namespace: openl-tablets
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: rule-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 5. OpenL Studio Deployment

```yaml
# studio-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: openl-studio
  namespace: openl-tablets
  labels:
    app: openl-studio
spec:
  replicas: 1
  selector:
    matchLabels:
      app: openl-studio
  template:
    metadata:
      labels:
        app: openl-studio
    spec:
      containers:
      - name: studio
        image: openltablets/openl-studio:6.0.0
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres-service:5432/openl_prod"
        - name: SPRING_DATASOURCE_USERNAME
          value: "openl"
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: openl-secrets
              key: db-password
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: JAVA_OPTS
          value: "-Xms1g -Xmx4g -XX:+UseG1GC"
        volumeMounts:
        - name: studio-data
          mountPath: /opt/openl/data
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 90
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "6Gi"
            cpu: "4000m"
      volumes:
      - name: studio-data
        persistentVolumeClaim:
          claimName: studio-pvc
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: studio-pvc
  namespace: openl-tablets
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
---
apiVersion: v1
kind: Service
metadata:
  name: openl-studio
  namespace: openl-tablets
spec:
  type: ClusterIP
  selector:
    app: openl-studio
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
    name: http
```

### 6. Ingress

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: openl-ingress
  namespace: openl-tablets
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "300"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "300"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - openl.your-domain.com
    secretName: openl-tls
  rules:
  - host: openl.your-domain.com
    http:
      paths:
      - path: /studio
        pathType: Prefix
        backend:
          service:
            name: openl-studio
            port:
              number: 80
      - path: /rules
        pathType: Prefix
        backend:
          service:
            name: rule-service
            port:
              number: 80
```

### Deploy to Kubernetes

```bash
# Create namespace
kubectl apply -f namespace.yaml

# Apply configurations
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml

# Deploy database
kubectl apply -f postgres-deployment.yaml

# Wait for database to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n openl-tablets --timeout=300s

# Deploy applications
kubectl apply -f rule-service-deployment.yaml
kubectl apply -f studio-deployment.yaml

# Configure ingress
kubectl apply -f ingress.yaml

# Verify deployments
kubectl get all -n openl-tablets

# View logs
kubectl logs -f deployment/rule-service -n openl-tablets
kubectl logs -f deployment/openl-studio -n openl-tablets
```

---

## Cloud Platform Deployments

### AWS Elastic Beanstalk

#### Prerequisites
- AWS CLI configured
- EB CLI installed
- WAR files built

#### Steps

```bash
# Initialize EB application
eb init -p corretto-21 openl-tablets --region us-east-1

# Create environment
eb create openl-prod \
  --instance-type t3.medium \
  --database.engine postgres \
  --database.size 20 \
  --envvars \
    SPRING_DATASOURCE_URL=jdbc:postgresql://[RDS_ENDPOINT]:5432/openl,\
    SPRING_DATASOURCE_USERNAME=openl,\
    SPRING_DATASOURCE_PASSWORD=[PASSWORD]

# Deploy
eb deploy

# Open in browser
eb open
```

#### Configuration (`.ebextensions/openl.config`)

```yaml
option_settings:
  aws:elasticbeanstalk:application:environment:
    JAVA_HOME: /usr/lib/jvm/java-21
    SPRING_PROFILES_ACTIVE: production
  aws:elasticbeanstalk:environment:proxy:
    ProxyServer: nginx
  aws:autoscaling:launchconfiguration:
    IamInstanceProfile: aws-elasticbeanstalk-ec2-role
    InstanceType: t3.medium
    RootVolumeSize: 20
  aws:elasticbeanstalk:cloudwatch:logs:
    StreamLogs: true
    DeleteOnTerminate: false
    RetentionInDays: 7
```

### Azure App Service

#### Steps

```bash
# Login to Azure
az login

# Create resource group
az group create --name openl-rg --location eastus

# Create PostgreSQL
az postgres flexible-server create \
  --resource-group openl-rg \
  --name openl-postgres \
  --location eastus \
  --admin-user openl \
  --admin-password [PASSWORD] \
  --sku-name Standard_B2s \
  --tier Burstable \
  --storage-size 32

# Create App Service Plan
az appservice plan create \
  --name openl-plan \
  --resource-group openl-rg \
  --sku P1V2 \
  --is-linux

# Create Web App
az webapp create \
  --resource-group openl-rg \
  --plan openl-plan \
  --name openl-studio \
  --runtime "JAVA:21-java21"

# Configure app settings
az webapp config appsettings set \
  --resource-group openl-rg \
  --name openl-studio \
  --settings \
    SPRING_DATASOURCE_URL="jdbc:postgresql://openl-postgres.postgres.database.azure.com:5432/openl" \
    SPRING_DATASOURCE_USERNAME="openl" \
    SPRING_DATASOURCE_PASSWORD="[PASSWORD]"

# Deploy WAR file
az webapp deploy \
  --resource-group openl-rg \
  --name openl-studio \
  --src-path STUDIO/org.openl.rules.webstudio/target/webapp.war \
  --type war
```

### Google Cloud Platform (GCP)

#### Using Cloud Run

```bash
# Set project
gcloud config set project your-project-id

# Build and push container
gcloud builds submit --tag gcr.io/your-project-id/openl-studio:latest

# Deploy to Cloud Run
gcloud run deploy openl-studio \
  --image gcr.io/your-project-id/openl-studio:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --memory 4Gi \
  --cpu 2 \
  --timeout 300 \
  --max-instances 10 \
  --set-env-vars="SPRING_DATASOURCE_URL=jdbc:postgresql://[CLOUD_SQL_IP]:5432/openl" \
  --set-secrets="DB_PASSWORD=openl-db-password:latest"

# Get service URL
gcloud run services describe openl-studio --region us-central1 --format='value(status.url)'
```

---

## Traditional Application Server

### Tomcat Deployment

#### Prerequisites
- Apache Tomcat 10.x+
- JDK 21+

#### Steps

```bash
# 1. Build WAR files
mvn clean install -DskipTests

# 2. Copy WAR files to Tomcat
cp STUDIO/org.openl.rules.webstudio/target/webapp.war \
   $TOMCAT_HOME/webapps/studio.war

cp WSFrontend/org.openl.rules.ruleservice.ws/target/webapp.war \
   $TOMCAT_HOME/webapps/rules.war

# 3. Configure JAVA_OPTS in setenv.sh
cat > $TOMCAT_HOME/bin/setenv.sh <<'EOF'
export JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5432/openl \
  -Dspring.datasource.username=openl \
  -Dspring.datasource.password=openl \
  -Dspring.profiles.active=production"
EOF

chmod +x $TOMCAT_HOME/bin/setenv.sh

# 4. Start Tomcat
$TOMCAT_HOME/bin/startup.sh

# 5. Access applications
# OpenL Studio: http://localhost:8080/studio
# Rule Services: http://localhost:8080/rules
```

#### Tomcat Configuration (`server.xml`)

```xml
<Server port="8005" shutdown="SHUTDOWN">
  <Service name="Catalina">
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443"
               maxThreads="200"
               minSpareThreads="10"
               compression="on"
               compressionMinSize="2048"
               compressibleMimeType="text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json"/>

    <!-- HTTPS Connector -->
    <Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
               maxThreads="200" SSLEnabled="true">
      <SSLHostConfig>
        <Certificate certificateKeystoreFile="conf/keystore.jks"
                     certificateKeystorePassword="changeit"
                     type="RSA"/>
      </SSLHostConfig>
    </Connector>

    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps"
            unpackWARs="true" autoDeploy="true">
        <Valve className="org.apache.catalina.valves.AccessLogValve"
               directory="logs"
               prefix="localhost_access_log" suffix=".txt"
               pattern="%h %l %u %t &quot;%r&quot; %s %b"/>
      </Host>
    </Engine>
  </Service>
</Server>
```

### WildFly/JBoss Deployment

```bash
# 1. Build WAR files
mvn clean install -DskipTests

# 2. Deploy using CLI
$JBOSS_HOME/bin/jboss-cli.sh --connect

# Deploy OpenL Studio
deploy STUDIO/org.openl.rules.webstudio/target/webapp.war --name=studio.war

# Deploy Rule Service
deploy WSFrontend/org.openl.rules.ruleservice.ws/target/webapp.war --name=rules.war

# Verify deployments
deployment-info

# Exit CLI
quit
```

---

## Database Configuration

### PostgreSQL (Recommended)

#### Production Configuration

```properties
# application-production.properties

# Connection pool (HikariCP)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# PostgreSQL specific
spring.datasource.url=jdbc:postgresql://localhost:5432/openl_prod?ssl=true&sslmode=require
spring.datasource.username=openl
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.properties.hibernate.jdbc.batch_size=25
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Liquibase
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
```

#### Database Tuning

```sql
-- PostgreSQL configuration (postgresql.conf)

-- Memory settings (for 8GB RAM server)
shared_buffers = 2GB
effective_cache_size = 6GB
maintenance_work_mem = 512MB
work_mem = 16MB

-- Checkpoint settings
checkpoint_completion_target = 0.9
wal_buffers = 16MB
min_wal_size = 1GB
max_wal_size = 4GB

-- Query tuning
random_page_cost = 1.1  # For SSD
effective_io_concurrency = 200

-- Connection settings
max_connections = 100
```

### MySQL Configuration

```properties
# application-production.properties

spring.datasource.url=jdbc:mysql://localhost:3306/openl_prod?useSSL=true&requireSSL=true&serverTimezone=UTC
spring.datasource.username=openl
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=validate
```

#### MySQL Tuning (`my.cnf`)

```ini
[mysqld]
# InnoDB settings
innodb_buffer_pool_size = 2G
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT

# Connection settings
max_connections = 200
connect_timeout = 10
wait_timeout = 600

# Query cache (MySQL 5.7)
query_cache_type = 1
query_cache_size = 128M
```

### Database Backup

#### PostgreSQL Backup Script

```bash
#!/bin/bash
# backup-postgres.sh

BACKUP_DIR="/backup/openl"
DB_NAME="openl_prod"
DB_USER="openl"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/openl_$DATE.sql.gz"

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup with compression
pg_dump -U $DB_USER -h localhost $DB_NAME | gzip > $BACKUP_FILE

# Keep only last 7 days of backups
find $BACKUP_DIR -name "openl_*.sql.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_FILE"
```

#### Restore

```bash
# PostgreSQL restore
gunzip < backup.sql.gz | psql -U openl -h localhost openl_prod

# MySQL restore
gunzip < backup.sql.gz | mysql -u openl -p openl_prod
```

---

## Security Configuration

### SSL/TLS Configuration

#### Generate Self-Signed Certificate (Dev/Test)

```bash
keytool -genkeypair -alias openl -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650 \
  -dname "CN=openl.local, OU=IT, O=YourCompany, L=City, ST=State, C=US"
```

#### Spring Boot SSL Configuration

```properties
# application-production.properties

# SSL/TLS
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=openl

# Require HTTPS
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3
server.ssl.ciphers=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
```

### Authentication Configuration

#### SAML 2.0 Configuration

```properties
# SAML configuration
security.saml.enabled=true
security.saml.entity-id=openl-tablets
security.saml.metadata-url=https://idp.example.com/metadata.xml
security.saml.keystore-file=classpath:saml-keystore.jks
security.saml.keystore-password=${SAML_KEYSTORE_PASSWORD}
security.saml.key-alias=openl-saml
security.saml.key-password=${SAML_KEY_PASSWORD}
```

#### OAuth 2.0 / OIDC Configuration

```properties
# OAuth2 configuration
security.oauth2.enabled=true
security.oauth2.client.registration.okta.client-id=${OAUTH2_CLIENT_ID}
security.oauth2.client.registration.okta.client-secret=${OAUTH2_CLIENT_SECRET}
security.oauth2.client.registration.okta.scope=openid,profile,email
security.oauth2.client.provider.okta.issuer-uri=https://your-domain.okta.com/oauth2/default
```

#### LDAP Configuration

```properties
# LDAP configuration
security.ldap.enabled=true
security.ldap.url=ldap://ldap.example.com:389
security.ldap.base-dn=dc=example,dc=com
security.ldap.user-dn-pattern=uid={0},ou=people
security.ldap.group-search-base=ou=groups
security.ldap.group-search-filter=(member={0})
```

---

## Performance Tuning

### JVM Tuning

```bash
# Recommended JVM options for production

JAVA_OPTS="
  # Heap size (adjust based on available RAM)
  -Xms2g
  -Xmx4g

  # Garbage Collector (G1GC recommended)
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:G1HeapRegionSize=16m
  -XX:InitiatingHeapOccupancyPercent=45

  # GC logging
  -Xlog:gc*:file=/var/log/openl/gc.log:time,uptime:filecount=5,filesize=10M

  # Memory management
  -XX:+UseStringDeduplication
  -XX:+ParallelRefProcEnabled

  # Performance
  -XX:+AlwaysPreTouch
  -XX:+DisableExplicitGC

  # Debugging (optional)
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/var/log/openl/heapdump.hprof

  # Networking
  -Djava.net.preferIPv4Stack=true
"
```

### Application Tuning

```properties
# application-production.properties

# Server
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10
server.tomcat.max-connections=8192
server.tomcat.accept-count=100
server.compression.enabled=true
server.compression.min-response-size=2048

# Database connection pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

# Caching
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=10000,expireAfterWrite=3600s

# Async processing
spring.task.execution.pool.core-size=8
spring.task.execution.pool.max-size=16
spring.task.execution.pool.queue-capacity=100
```

### Rule Compilation Caching

```properties
# Cache compiled rules
ruleservice.instantiation.strategy.lazy=true
ruleservice.datasource.deploy.clean.datasource=false

# Parallel compilation
openl.parallel.compilation.enabled=true
openl.parallel.compilation.threads=4
```

---

## Monitoring and Observability

### Spring Boot Actuator

```properties
# application-production.properties

# Actuator endpoints
management.endpoints.web.exposure.include=health,metrics,prometheus,info,env
management.endpoint.health.show-details=when-authorized
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true

# Metrics
management.metrics.enable.jvm=true
management.metrics.enable.process=true
management.metrics.enable.system=true
management.metrics.export.prometheus.enabled=true

# Info endpoint
management.info.env.enabled=true
management.info.build.enabled=true
management.info.git.enabled=true
```

### Prometheus Configuration

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'openl-rule-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['rule-service:8080']
        labels:
          application: 'rule-service'
          environment: 'production'

  - job_name: 'openl-studio'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['openl-studio:8080']
        labels:
          application: 'openl-studio'
          environment: 'production'
```

### Grafana Dashboard

Import pre-built Spring Boot dashboards:
- Dashboard ID: 4701 (JVM Micrometer)
- Dashboard ID: 11378 (JVM Quick Start)

Custom metrics to monitor:
- Rule execution time
- Compilation time
- Cache hit ratio
- Active sessions
- Request rate/errors

### Logging

```properties
# application-production.properties

# Logging levels
logging.level.root=WARN
logging.level.org.openl=INFO
logging.level.org.springframework=INFO
logging.level.org.hibernate=WARN

# Log file
logging.file.name=/var/log/openl/application.log
logging.file.max-size=100MB
logging.file.max-history=30

# Log pattern
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# JSON logging (for log aggregation)
logging.appender.rolling.encoder.class=net.logstash.logback.encoder.LogstashEncoder
```

### ELK Stack Integration

```yaml
# filebeat.yml
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/openl/*.log
  fields:
    application: openl-tablets
    environment: production
  multiline:
    pattern: '^[0-9]{4}-[0-9]{2}-[0-9]{2}'
    negate: true
    match: after

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "openl-%{+yyyy.MM.dd}"

setup.kibana:
  host: "kibana:5601"
```

---

## High Availability

### Load Balancing Strategy

```
┌─────────────────────┐
│   Load Balancer     │
│   (HAProxy/Nginx)   │
└──────────┬──────────┘
           │
     ┌─────┴─────┐
     │           │
┌────▼────┐ ┌───▼─────┐
│ Rule    │ │ Rule    │
│ Service │ │ Service │
│ Node 1  │ │ Node 2  │
└────┬────┘ └───┬─────┘
     │          │
     └────┬─────┘
          │
    ┌─────▼──────┐
    │  Database  │
    │ (Primary)  │
    └─────┬──────┘
          │
    ┌─────▼──────┐
    │  Database  │
    │ (Replica)  │
    └────────────┘
```

### HAProxy Configuration

```haproxy
# haproxy.cfg
global
    maxconn 4096

defaults
    mode http
    timeout connect 5000ms
    timeout client 50000ms
    timeout server 50000ms

frontend http_front
    bind *:80
    default_backend rule_services

backend rule_services
    balance roundrobin
    option httpchk GET /actuator/health
    http-check expect status 200
    server rule1 rule-service-1:8080 check
    server rule2 rule-service-2:8080 check
    server rule3 rule-service-3:8080 check

listen stats
    bind *:8404
    stats enable
    stats uri /stats
    stats refresh 30s
```

### Database Replication

#### PostgreSQL Streaming Replication

**Primary Server** (`postgresql.conf`):
```ini
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
hot_standby = on
```

**Replica Server**:
```bash
# Stop replica
pg_ctl stop

# Clone from primary
pg_basebackup -h primary-server -D /var/lib/postgresql/data -U replication -P -R

# Start replica
pg_ctl start
```

---

## Backup and Disaster Recovery

### Backup Strategy

| Backup Type | Frequency | Retention | Purpose |
|-------------|-----------|-----------|---------|
| **Full Backup** | Daily | 30 days | Complete system restore |
| **Incremental** | Hourly | 7 days | Point-in-time recovery |
| **Rule Files** | On change (Git) | Indefinite | Rule versioning |
| **Database** | Daily | 30 days | Data recovery |
| **Configuration** | On change | Indefinite | Configuration rollback |

### Automated Backup Script

```bash
#!/bin/bash
# backup-openl.sh

BACKUP_DIR="/backup/openl"
DATE=$(date +%Y%m%d_%H%M%S)

# 1. Backup database
pg_dump -U openl openl_prod | gzip > $BACKUP_DIR/db_$DATE.sql.gz

# 2. Backup rule files
tar -czf $BACKUP_DIR/rules_$DATE.tar.gz /opt/openl/rules

# 3. Backup configuration
tar -czf $BACKUP_DIR/config_$DATE.tar.gz /opt/openl/config

# 4. Upload to S3
aws s3 sync $BACKUP_DIR s3://your-backup-bucket/openl/

# 5. Clean old backups (keep 30 days)
find $BACKUP_DIR -mtime +30 -delete
```

### Disaster Recovery Plan

**RTO (Recovery Time Objective)**: 1 hour
**RPO (Recovery Point Objective)**: 1 hour

#### Recovery Steps

1. **Infrastructure Recovery** (15 minutes)
   - Provision new servers/containers
   - Configure networking
   - Install dependencies

2. **Database Recovery** (20 minutes)
   - Restore latest database backup
   - Apply transaction logs if available
   - Verify data integrity

3. **Application Recovery** (15 minutes)
   - Deploy OpenL Tablets WAR files
   - Restore configuration files
   - Restore rule files from Git

4. **Verification** (10 minutes)
   - Health checks
   - Smoke tests
   - Load balancer configuration
   - DNS update

---

## Troubleshooting

### Common Issues

#### 1. Out of Memory Errors

**Symptoms**:
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions**:
```bash
# Increase heap size
JAVA_OPTS="-Xms4g -Xmx8g"

# Enable heap dumps
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/openl"

# Analyze heap dump
jhat heapdump.hprof
# Or use Eclipse Memory Analyzer (MAT)
```

#### 2. Slow Rule Compilation

**Symptoms**: Rule compilation takes too long

**Solutions**:
```properties
# Enable parallel compilation
openl.parallel.compilation.enabled=true
openl.parallel.compilation.threads=4

# Increase compiler cache
openl.compiler.cache.size=1000
```

#### 3. Database Connection Errors

**Symptoms**:
```
Unable to acquire JDBC Connection
```

**Solutions**:
```properties
# Increase connection pool
spring.datasource.hikari.maximum-pool-size=50

# Increase timeouts
spring.datasource.hikari.connection-timeout=60000

# Validate connections
spring.datasource.hikari.connection-test-query=SELECT 1
```

#### 4. High CPU Usage

**Causes**:
- Inefficient rules
- Too many concurrent requests
- GC thrashing

**Investigation**:
```bash
# Thread dump
jstack <pid> > thread-dump.txt

# CPU profiling
java -agentlib:hprof=cpu=samples <app>

# Monitor GC
jstat -gc <pid> 1000
```

### Health Check Endpoints

```bash
# Liveness probe (is the app running?)
curl http://localhost:8080/actuator/health/liveness

# Readiness probe (can it handle requests?)
curl http://localhost:8080/actuator/health/readiness

# Full health
curl http://localhost:8080/actuator/health
```

### Performance Profiling

```bash
# Enable JFR (Java Flight Recorder)
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr <app>

# Analyze with JMC (Java Mission Control)
jmc recording.jfr
```

---

## Additional Resources

- [OpenL Tablets Documentation](https://openl-tablets.org/documentation)
- [Performance Tuning Guide](../guides/performance-tuning.md)
- [Security Best Practices](../guides/security-best-practices.md)
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)

---

**Need Help?** Open an issue on [GitHub](https://github.com/openl-tablets/openl-tablets/issues) or consult the [community forums](https://github.com/openl-tablets/openl-tablets/discussions).

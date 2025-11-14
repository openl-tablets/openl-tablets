# Kubernetes Deployment

Comprehensive guide for deploying OpenL Tablets on Kubernetes clusters using Helm charts and Kubernetes manifests.

## Overview

Kubernetes provides enterprise-grade container orchestration for OpenL Tablets with:

- **Automatic scaling**: Horizontal Pod Autoscaling (HPA)
- **Self-healing**: Automatic restart and rescheduling
- **Load balancing**: Built-in service discovery and load balancing
- **Rolling updates**: Zero-downtime deployments
- **Resource management**: CPU/memory limits and requests
- **High availability**: Pod distribution across nodes

## Prerequisites

### Required Tools

```bash
# Kubernetes CLI
kubectl version --client

# Helm (package manager)
helm version

# Optional: k9s (cluster management)
k9s version
```

### Cluster Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **Kubernetes** | v1.24+ | v1.28+ |
| **Nodes** | 3 nodes | 5+ nodes |
| **CPU** | 8 cores total | 16+ cores |
| **Memory** | 16GB total | 32GB+ |
| **Storage** | 100GB | 500GB+ |
| **Ingress Controller** | Required | Nginx/Traefik |
| **Storage Class** | Required | Dynamic provisioning |

### Access Requirements

```bash
# Verify cluster access
kubectl cluster-info

# Check available nodes
kubectl get nodes

# Verify storage classes
kubectl get storageclass
```

---

## Quick Start

### Using Helm (Recommended)

```bash
# Add Helm repository
helm repo add openl-tablets https://openl-tablets.github.io/helm-charts
helm repo update

# Install with default values
helm install openl-tablets openl-tablets/openl-tablets \
  --namespace openl \
  --create-namespace

# Check deployment status
kubectl get pods -n openl

# Get service URLs
kubectl get ingress -n openl
```

### Using Kubernetes Manifests

```bash
# Create namespace
kubectl create namespace openl

# Apply manifests
kubectl apply -f manifests/ -n openl

# Check status
kubectl get all -n openl
```

---

## Deployment Options

### Option 1: Basic Deployment

**Purpose**: Development, testing, small production

**Components**:
- 1 OpenL Studio replica
- 1 OpenL Rule Services replica
- PostgreSQL (single instance)

**Install**:
```bash
helm install openl-tablets openl-tablets/openl-tablets \
  --namespace openl \
  --create-namespace \
  --set studio.replicaCount=1 \
  --set ruleservices.replicaCount=1 \
  --set postgresql.enabled=true
```

### Option 2: High Availability

**Purpose**: Production with redundancy

**Components**:
- 3 OpenL Studio replicas
- 3 OpenL Rule Services replicas
- PostgreSQL HA (primary + replica)
- Redis for caching

**Install**:
```bash
helm install openl-tablets openl-tablets/openl-tablets \
  --namespace openl \
  --create-namespace \
  --set studio.replicaCount=3 \
  --set ruleservices.replicaCount=3 \
  --set postgresql.replication.enabled=true \
  --set redis.enabled=true \
  --set ingress.enabled=true
```

### Option 3: Auto-Scaling Production

**Purpose**: Large-scale production with auto-scaling

**Components**:
- Studio: 2-10 replicas (auto-scaled)
- Rule Services: 3-20 replicas (auto-scaled)
- PostgreSQL HA cluster
- Redis cluster
- Monitoring stack (Prometheus/Grafana)

**Install**:
```bash
helm install openl-tablets openl-tablets/openl-tablets \
  --namespace openl \
  --create-namespace \
  --values values-production.yaml
```

Create `values-production.yaml`:
```yaml
# See Configuration section below
```

---

## Helm Chart Configuration

### Values File Structure

Create `values.yaml` to customize deployment:

```yaml
# Global settings
global:
  storageClass: "standard"
  imagePullPolicy: IfNotPresent

# OpenL Studio
studio:
  enabled: true
  replicaCount: 3

  image:
    repository: openltablets/studio
    tag: "latest"

  resources:
    requests:
      memory: "2Gi"
      cpu: "1000m"
    limits:
      memory: "4Gi"
      cpu: "2000m"

  env:
    SECURITY_MODE: "multi-user"
    ADMIN_USERNAME: "admin"
    ADMIN_PASSWORD: "changeme"  # Use secrets in production!

  persistence:
    enabled: true
    size: "50Gi"
    accessMode: ReadWriteMany

  autoscaling:
    enabled: true
    minReplicas: 2
    maxReplicas: 10
    targetCPUUtilizationPercentage: 70
    targetMemoryUtilizationPercentage: 80

# OpenL Rule Services
ruleservices:
  enabled: true
  replicaCount: 3

  image:
    repository: openltablets/ruleservice
    tag: "latest"

  resources:
    requests:
      memory: "4Gi"
      cpu: "2000m"
    limits:
      memory: "8Gi"
      cpu: "4000m"

  env:
    RULESERVICE_API_ENABLED: "true"
    RULESERVICE_CACHE_ENABLED: "true"

  persistence:
    enabled: true
    size: "100Gi"
    accessMode: ReadWriteMany

  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 20
    targetCPUUtilizationPercentage: 70

# PostgreSQL Database
postgresql:
  enabled: true
  auth:
    username: openl
    password: changeme  # Use secrets in production!
    database: openl

  primary:
    persistence:
      enabled: true
      size: "100Gi"
    resources:
      requests:
        memory: "2Gi"
        cpu: "1000m"
      limits:
        memory: "4Gi"
        cpu: "2000m"

  replication:
    enabled: true
    replicaCount: 1

# Redis Cache
redis:
  enabled: true
  auth:
    password: "changeme"  # Use secrets in production!

  master:
    persistence:
      enabled: true
      size: "10Gi"

  replica:
    replicaCount: 2

# Ingress
ingress:
  enabled: true
  className: "nginx"

  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"

  hosts:
    - host: openl.example.com
      paths:
        - path: /
          pathType: Prefix
          service: studio
        - path: /ruleservices
          pathType: Prefix
          service: ruleservices

  tls:
    - secretName: openl-tls
      hosts:
        - openl.example.com

# Monitoring
monitoring:
  enabled: true

  prometheus:
    enabled: true
    retention: 30d
    storage: 50Gi

  grafana:
    enabled: true
    adminPassword: "changeme"
```

### Using Custom Values

```bash
# Install with custom values
helm install openl-tablets openl-tablets/openl-tablets \
  --namespace openl \
  --create-namespace \
  --values values.yaml

# Upgrade with new values
helm upgrade openl-tablets openl-tablets/openl-tablets \
  --namespace openl \
  --values values.yaml
```

---

## Kubernetes Manifests

For deployments without Helm, use Kubernetes manifests directly.

### Directory Structure

```
manifests/
├── namespace.yaml
├── configmap.yaml
├── secrets.yaml
├── postgresql/
│   ├── statefulset.yaml
│   ├── service.yaml
│   └── pvc.yaml
├── redis/
│   ├── deployment.yaml
│   ├── service.yaml
│   └── pvc.yaml
├── studio/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── pvc.yaml
│   ├── hpa.yaml
│   └── ingress.yaml
├── ruleservices/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── pvc.yaml
│   └── hpa.yaml
└── monitoring/
    ├── prometheus/
    └── grafana/
```

### Example Manifests

See [manifests/](manifests/) directory for complete examples:
- [Studio Deployment](manifests/studio-deployment.yaml)
- [Rule Services Deployment](manifests/ruleservices-deployment.yaml)
- [PostgreSQL StatefulSet](manifests/postgresql-statefulset.yaml)
- [Ingress Configuration](manifests/ingress.yaml)

---

## Storage Configuration

### Persistent Volumes

OpenL Tablets requires persistent storage for:

| Component | Purpose | Access Mode | Size |
|-----------|---------|-------------|------|
| **Studio Workspace** | Projects, repositories | ReadWriteMany | 50-200GB |
| **Rule Services Deployments** | Rule artifacts | ReadWriteMany | 100-500GB |
| **PostgreSQL Data** | Database storage | ReadWriteOnce | 100-1TB |
| **Redis Data** | Cache storage | ReadWriteOnce | 10-50GB |

### Storage Class Configuration

#### AWS EBS

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ebs-gp3
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  iops: "3000"
  throughput: "125"
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

#### Azure Disk

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: azure-disk-premium
provisioner: kubernetes.io/azure-disk
parameters:
  storageaccounttype: Premium_LRS
  kind: Managed
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

#### NFS (for ReadWriteMany)

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: nfs-storage
provisioner: nfs-subdir-external-provisioner
parameters:
  archiveOnDelete: "false"
```

### Using Specific Storage Class

```yaml
# In values.yaml
global:
  storageClass: "ebs-gp3"  # or "azure-disk-premium"

studio:
  persistence:
    storageClass: "nfs-storage"  # Override for specific component
```

---

## Networking

### Service Configuration

```yaml
# Studio Service (ClusterIP)
apiVersion: v1
kind: Service
metadata:
  name: openl-studio
  namespace: openl
spec:
  type: ClusterIP
  ports:
    - port: 8080
      targetPort: 8080
      name: http
  selector:
    app: openl-studio

# Rule Services (ClusterIP)
apiVersion: v1
kind: Service
metadata:
  name: openl-ruleservices
  namespace: openl
spec:
  type: ClusterIP
  ports:
    - port: 8080
      targetPort: 8080
      name: http
  selector:
    app: openl-ruleservices
```

### Ingress Configuration

#### Nginx Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: openl-ingress
  namespace: openl
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "100m"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - openl.example.com
      secretName: openl-tls
  rules:
    - host: openl.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: openl-studio
                port:
                  number: 8080
          - path: /ruleservices
            pathType: Prefix
            backend:
              service:
                name: openl-ruleservices
                port:
                  number: 8080
```

#### Traefik Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: openl-ingress
  namespace: openl
  annotations:
    traefik.ingress.kubernetes.io/router.entrypoints: websecure
    traefik.ingress.kubernetes.io/router.tls: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: traefik
  tls:
    - hosts:
        - openl.example.com
      secretName: openl-tls
  rules:
    - host: openl.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: openl-studio
                port:
                  number: 8080
          - path: /ruleservices
            pathType: Prefix
            backend:
              service:
                name: openl-ruleservices
                port:
                  number: 8080
```

### Network Policies

Restrict traffic between pods:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: openl-network-policy
  namespace: openl
spec:
  podSelector:
    matchLabels:
      app: openl-studio
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: nginx-ingress
      ports:
        - protocol: TCP
          port: 8080
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: postgresql
      ports:
        - protocol: TCP
          port: 5432
    - to:
        - podSelector:
            matchLabels:
              app: redis
      ports:
        - protocol: TCP
          port: 6379
```

---

## Security

### Secrets Management

#### Using Kubernetes Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: openl-secrets
  namespace: openl
type: Opaque
stringData:
  admin-password: "your-secure-password"
  db-password: "your-db-password"
  redis-password: "your-redis-password"
```

#### Using External Secrets Operator

```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
  namespace: openl
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        secretRef:
          accessKeyIDSecretRef:
            name: aws-credentials
            key: access-key-id
          secretAccessKeySecretRef:
            name: aws-credentials
            key: secret-access-key

---
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: openl-external-secrets
  namespace: openl
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: openl-secrets
    creationPolicy: Owner
  data:
    - secretKey: admin-password
      remoteRef:
        key: openl/admin-password
    - secretKey: db-password
      remoteRef:
        key: openl/db-password
```

### Pod Security Standards

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: openl
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
```

### Security Context

```yaml
# In Deployment
spec:
  template:
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
        seccompProfile:
          type: RuntimeDefault

      containers:
        - name: studio
          securityContext:
            allowPrivilegeEscalation: false
            capabilities:
              drop:
                - ALL
            readOnlyRootFilesystem: false  # OpenL needs write access
```

---

## Auto-Scaling

### Horizontal Pod Autoscaler (HPA)

#### Studio HPA

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: openl-studio-hpa
  namespace: openl
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: openl-studio
  minReplicas: 2
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
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 50
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 25
          periodSeconds: 60
```

#### Rule Services HPA

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: openl-ruleservices-hpa
  namespace: openl
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: openl-ruleservices
  minReplicas: 3
  maxReplicas: 20
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
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "1000"
```

### Vertical Pod Autoscaler (VPA)

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: openl-studio-vpa
  namespace: openl
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: openl-studio
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
      - containerName: studio
        minAllowed:
          cpu: 500m
          memory: 1Gi
        maxAllowed:
          cpu: 4000m
          memory: 8Gi
```

---

## Monitoring

### Prometheus ServiceMonitor

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: openl-studio-monitor
  namespace: openl
  labels:
    app: openl-studio
spec:
  selector:
    matchLabels:
      app: openl-studio
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 30s

---
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: openl-ruleservices-monitor
  namespace: openl
  labels:
    app: openl-ruleservices
spec:
  selector:
    matchLabels:
      app: openl-ruleservices
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 30s
```

### Grafana Dashboards

Import dashboard via ConfigMap:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-dashboard-openl
  namespace: monitoring
  labels:
    grafana_dashboard: "1"
data:
  openl-tablets.json: |
    {
      "dashboard": {
        "title": "OpenL Tablets Overview",
        "panels": [...]
      }
    }
```

### Alerts

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: openl-alerts
  namespace: openl
spec:
  groups:
    - name: openl
      interval: 30s
      rules:
        - alert: HighMemoryUsage
          expr: |
            (sum(container_memory_working_set_bytes{namespace="openl",container="studio"})
            / sum(container_spec_memory_limit_bytes{namespace="openl",container="studio"})) > 0.9
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "High memory usage in OpenL Studio"
            description: "Memory usage is above 90% for 5 minutes"

        - alert: PodCrashLooping
          expr: |
            rate(kube_pod_container_status_restarts_total{namespace="openl"}[15m]) > 0
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "Pod is crash looping"
            description: "Pod {{ $labels.pod }} is restarting frequently"
```

---

## Backup and Restore

### Database Backup

#### Using CronJob

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
  namespace: openl
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: backup
              image: postgres:16-alpine
              env:
                - name: PGHOST
                  value: "openl-postgresql"
                - name: PGUSER
                  value: "openl"
                - name: PGPASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: openl-secrets
                      key: db-password
              command:
                - /bin/sh
                - -c
                - |
                  DATE=$(date +%Y%m%d-%H%M%S)
                  pg_dump openl | gzip > /backups/backup-$DATE.sql.gz
                  # Upload to S3 or other storage
              volumeMounts:
                - name: backups
                  mountPath: /backups
          volumes:
            - name: backups
              persistentVolumeClaim:
                claimName: backup-pvc
          restartPolicy: OnFailure
```

#### Using Velero

```bash
# Install Velero
velero install \
  --provider aws \
  --plugins velero/velero-plugin-for-aws:v1.8.0 \
  --bucket openl-backups \
  --backup-location-config region=us-east-1 \
  --snapshot-location-config region=us-east-1

# Create backup
velero backup create openl-backup --include-namespaces openl

# Restore
velero restore create --from-backup openl-backup
```

### Workspace Backup

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: workspace-backup
  namespace: openl
spec:
  schedule: "0 3 * * *"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: backup
              image: alpine:latest
              command:
                - /bin/sh
                - -c
                - |
                  DATE=$(date +%Y%m%d-%H%M%S)
                  tar czf /backups/workspace-$DATE.tar.gz -C /workspace .
                  # Upload to S3
                  apk add --no-cache aws-cli
                  aws s3 cp /backups/workspace-$DATE.tar.gz s3://openl-backups/
              volumeMounts:
                - name: workspace
                  mountPath: /workspace
                  readOnly: true
                - name: backups
                  mountPath: /backups
          volumes:
            - name: workspace
              persistentVolumeClaim:
                claimName: studio-workspace-pvc
            - name: backups
              emptyDir: {}
          restartPolicy: OnFailure
```

---

## Upgrading

### Helm Upgrade

```bash
# Check current version
helm list -n openl

# Update Helm repository
helm repo update

# Upgrade with new version
helm upgrade openl-tablets openl-tablets/openl-tablets \
  --namespace openl \
  --values values.yaml \
  --version 6.0.0

# Rollback if needed
helm rollback openl-tablets -n openl
```

### Rolling Update Strategy

```yaml
# In Deployment
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0  # Ensure zero downtime
```

---

## Troubleshooting

### Pod Issues

```bash
# Check pod status
kubectl get pods -n openl

# View pod logs
kubectl logs -n openl <pod-name> -f

# Previous container logs
kubectl logs -n openl <pod-name> --previous

# Describe pod (events, status)
kubectl describe pod -n openl <pod-name>

# Shell into pod
kubectl exec -it -n openl <pod-name> -- /bin/bash
```

### Service Connectivity

```bash
# Test service from another pod
kubectl run -it --rm debug --image=busybox --restart=Never -n openl -- sh
# Inside pod:
wget -O- http://openl-studio:8080/actuator/health

# Port forward for local testing
kubectl port-forward -n openl svc/openl-studio 8080:8080
```

### Storage Issues

```bash
# Check PVC status
kubectl get pvc -n openl

# View PVC details
kubectl describe pvc -n openl <pvc-name>

# Check storage class
kubectl get storageclass
```

### Resource Issues

```bash
# View resource usage
kubectl top pods -n openl
kubectl top nodes

# Check resource quotas
kubectl get resourcequota -n openl

# View HPA status
kubectl get hpa -n openl
kubectl describe hpa -n openl openl-studio-hpa
```

---

## Best Practices

### Production Checklist

✅ **Configuration**:
- Use external PostgreSQL (not in-cluster for large deployments)
- Enable Redis for session management and caching
- Configure persistent storage with backups
- Set resource requests and limits
- Enable auto-scaling (HPA)

✅ **Security**:
- Use secrets management (External Secrets, Sealed Secrets)
- Enable network policies
- Configure pod security standards
- Use TLS/SSL for all connections
- Enable RBAC with least privilege

✅ **High Availability**:
- Run at least 3 replicas of each component
- Use pod anti-affinity for node distribution
- Configure pod disruption budgets
- Set up health checks and readiness probes

✅ **Monitoring**:
- Enable Prometheus metrics
- Configure Grafana dashboards
- Set up alerting rules
- Monitor resource usage and scaling

✅ **Backup**:
- Automated daily backups
- Test restore procedures
- Backup to external storage (S3, GCS)
- Document disaster recovery plan

---

## Related Documentation

- [Deployment Overview](../) - All deployment options
- [Docker Deployment](../docker/) - Container basics
- [Cloud Deployment](../cloud/) - AWS, Azure, GCP
- [Configuration Reference](../../configuration/) - Configuration options
- [Security Guide](../../configuration/security.md) - Security best practices

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

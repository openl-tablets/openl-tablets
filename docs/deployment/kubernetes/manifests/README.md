# Kubernetes Manifests for OpenL Tablets

This directory contains example Kubernetes manifests for deploying OpenL Tablets.

## Quick Start

```bash
# Create namespace and apply all manifests
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-secrets.yaml
kubectl apply -f studio-deployment.yaml
kubectl apply -f ruleservices-deployment.yaml
kubectl apply -f ingress.yaml

# Check deployment status
kubectl get all -n openl

# View logs
kubectl logs -n openl -l app=openl-studio -f
```

## Files Overview

| File | Description | Resources |
|------|-------------|-----------|
| `00-namespace.yaml` | Namespace, ResourceQuota, LimitRange | 3 |
| `01-secrets.yaml` | Secrets configuration | 1 |
| `studio-deployment.yaml` | OpenL Studio deployment | 5 (Deployment, Service, PVC, HPA, PDB) |
| `ruleservices-deployment.yaml` | Rule Services deployment | 5 (Deployment, Service, PVC, HPA, PDB) |
| `ingress.yaml` | Ingress for HTTP/HTTPS access | 1 |

## Prerequisites

Before deploying:

1. **Kubernetes cluster** (v1.24+)
2. **kubectl** configured
3. **Ingress controller** (Nginx or Traefik)
4. **Storage class** with ReadWriteMany support (for shared volumes)
5. **cert-manager** (optional, for TLS certificates)

### Check Prerequisites

```bash
# Check cluster version
kubectl version

# Check storage classes
kubectl get storageclass

# Check ingress controller
kubectl get pods -n ingress-nginx
# or
kubectl get pods -n traefik
```

## Deployment Steps

### Step 1: Create Namespace

```bash
kubectl apply -f 00-namespace.yaml
```

This creates:
- `openl` namespace
- Resource quotas (CPU, memory, PVC limits)
- Limit ranges (default resource requests/limits)

### Step 2: Configure Secrets

⚠️ **IMPORTANT**: Update passwords before deploying!

Edit `01-secrets.yaml` and change:
- `db-password`
- `admin-password`
- `redis-password`

```bash
# Apply secrets
kubectl apply -f 01-secrets.yaml

# Verify
kubectl get secret openl-secrets -n openl
```

**Production Recommendation**: Use External Secrets Operator or Sealed Secrets instead of plain Kubernetes Secrets.

### Step 3: Deploy Database

**Option A**: Deploy PostgreSQL in-cluster (testing only)

```bash
# Use bitnami/postgresql Helm chart
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install openl-postgresql bitnami/postgresql \
  --namespace openl \
  --set auth.username=openl \
  --set auth.password=changeme \
  --set auth.database=openl \
  --set primary.persistence.size=100Gi
```

**Option B**: Use external PostgreSQL (recommended for production)

Update `DATABASE_URL` in deployment manifests to point to external database.

### Step 4: Deploy Redis

```bash
# Use bitnami/redis Helm chart
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install openl-redis bitnami/redis \
  --namespace openl \
  --set auth.password=changeme \
  --set master.persistence.size=10Gi \
  --set replica.replicaCount=2
```

### Step 5: Deploy OpenL Studio

```bash
kubectl apply -f studio-deployment.yaml

# Wait for deployment
kubectl wait --for=condition=available --timeout=300s \
  deployment/openl-studio -n openl

# Check pods
kubectl get pods -n openl -l app=openl-studio
```

### Step 6: Deploy Rule Services

```bash
kubectl apply -f ruleservices-deployment.yaml

# Wait for deployment
kubectl wait --for=condition=available --timeout=300s \
  deployment/openl-ruleservices -n openl

# Check pods
kubectl get pods -n openl -l app=openl-ruleservices
```

### Step 7: Configure Ingress

Edit `ingress.yaml`:
- Change `openl.example.com` to your domain
- Update TLS configuration if needed

```bash
kubectl apply -f ingress.yaml

# Check ingress
kubectl get ingress -n openl
kubectl describe ingress openl-ingress -n openl
```

### Step 8: Access OpenL Tablets

```bash
# Get ingress address
kubectl get ingress -n openl

# Add to /etc/hosts if using local testing
echo "<INGRESS-IP> openl.example.com" | sudo tee -a /etc/hosts

# Access in browser
open https://openl.example.com
```

## Configuration

### Storage Classes

Update storage class in manifests if needed:

```yaml
# In studio-deployment.yaml and ruleservices-deployment.yaml
spec:
  storageClassName: nfs-storage  # Change to your storage class
```

Common storage classes:
- AWS: `gp3`, `ebs-csi`
- Azure: `managed-premium`, `azurefile`
- GCP: `standard-rwo`, `standard-rwx`
- On-premises: `nfs-storage`, `longhorn`

### Resource Sizing

Adjust resources based on workload:

```yaml
# In deployment manifests
resources:
  requests:
    memory: "2Gi"  # Minimum
    cpu: "1000m"
  limits:
    memory: "4Gi"  # Maximum
    cpu: "2000m"
```

### Auto-Scaling

Adjust HPA settings:

```yaml
# In studio-deployment.yaml or ruleservices-deployment.yaml
spec:
  minReplicas: 2      # Minimum pods
  maxReplicas: 10     # Maximum pods
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          averageUtilization: 70  # Target CPU %
```

## Monitoring

### Check Deployment Status

```bash
# All resources
kubectl get all -n openl

# Pods
kubectl get pods -n openl -o wide

# Services
kubectl get svc -n openl

# Ingress
kubectl get ingress -n openl

# PVCs
kubectl get pvc -n openl

# HPA
kubectl get hpa -n openl
```

### View Logs

```bash
# Studio logs
kubectl logs -n openl -l app=openl-studio -f

# Rule Services logs
kubectl logs -n openl -l app=openl-ruleservices -f

# Specific pod
kubectl logs -n openl <pod-name> -f

# Previous container (if restarted)
kubectl logs -n openl <pod-name> --previous
```

### Events

```bash
# Namespace events
kubectl get events -n openl --sort-by='.lastTimestamp'

# Pod events
kubectl describe pod -n openl <pod-name>
```

### Metrics

```bash
# Pod resource usage
kubectl top pods -n openl

# Node resource usage
kubectl top nodes
```

## Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl describe pod -n openl <pod-name>

# Common issues:
# - Image pull errors: Check image name and registry access
# - Volume mount errors: Check PVC status
# - Resource limits: Check node resources
```

### Database Connection Errors

```bash
# Test connectivity from pod
kubectl exec -it -n openl <studio-pod> -- sh
nc -zv openl-postgresql 5432

# Check secrets
kubectl get secret openl-secrets -n openl -o yaml
```

### Ingress Not Working

```bash
# Check ingress controller
kubectl get pods -n ingress-nginx

# Check ingress status
kubectl describe ingress openl-ingress -n openl

# Test service directly
kubectl port-forward -n openl svc/openl-studio 8080:8080
open http://localhost:8080
```

### Storage Issues

```bash
# Check PVC status
kubectl get pvc -n openl
kubectl describe pvc -n openl <pvc-name>

# Check storage class
kubectl get storageclass
kubectl describe storageclass <class-name>
```

## Cleanup

```bash
# Delete all OpenL Tablets resources
kubectl delete -f .

# Delete namespace (removes everything)
kubectl delete namespace openl

# Delete Helm releases if used
helm uninstall openl-postgresql -n openl
helm uninstall openl-redis -n openl
```

## Production Considerations

### High Availability

- Run at least 3 replicas of Studio and Rule Services
- Use pod anti-affinity to distribute across nodes
- Configure pod disruption budgets
- Use external, highly-available database

### Security

- Use external secrets management (not plain Secrets)
- Enable network policies
- Use pod security standards
- Configure RBAC with least privilege
- Enable TLS/SSL for all connections

### Backup

- Set up automated database backups (CronJob or Velero)
- Backup persistent volumes
- Test restore procedures
- Document disaster recovery plan

### Monitoring

- Deploy Prometheus and Grafana
- Configure ServiceMonitors
- Set up alerting rules
- Monitor resource usage and scaling

## Related Documentation

- [Kubernetes Deployment Guide](../index.md) - Comprehensive guide
- [Helm Chart](../helm/) - Helm-based deployment
- [Configuration Reference](../../configuration/) - Configuration options
- [Security Guide](../../configuration/security.md) - Security best practices

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

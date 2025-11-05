# OpenL Tablets Deployment Guide

Comprehensive deployment documentation for OpenL Tablets across various platforms and environments.

## Deployment Options

OpenL Tablets can be deployed in multiple ways depending on your requirements:

| Deployment Type | Use Case | Complexity | Scalability |
|----------------|----------|------------|-------------|
| [Docker Compose](docker/) | Development, testing, small production | Low | Single node |
| [Kubernetes/Helm](kubernetes/) | Production, enterprise | Medium | High |
| [AWS](cloud/aws/) | Cloud-native AWS deployment | Medium-High | High |
| [Azure](cloud/azure/) | Cloud-native Azure deployment | Medium-High | High |
| [VM/Bare Metal](vm/) | Traditional deployment | Medium | Manual |

## Quick Start

### Docker Compose (Recommended for Testing)

```bash
# Clone repository
git clone https://github.com/openl-tablets/openl-tablets.git
cd openl-tablets

# Start with docker-compose
docker-compose -f docs/deployment/docker/docker-compose-simple.yaml up -d

# Access OpenL Studio
open http://localhost:8080
```

### Kubernetes/Helm (Recommended for Production)

```bash
# Add Helm repository
helm repo add openl-tablets https://openl-tablets.github.io/helm-charts
helm repo update

# Install OpenL Tablets
helm install openl-tablets openl-tablets/openl-tablets \
  --set studio.enabled=true \
  --set ruleservices.enabled=true

# Access via LoadBalancer or Ingress
kubectl get svc openl-tablets-studio
```

## Architecture Overview

OpenL Tablets consists of several components:

### Core Components

1. **OpenL Studio** - Web-based IDE for rule development
   - Rule authoring and editing
   - Project management
   - Repository integration (Git)
   - User management

2. **OpenL Rule Services** - Rule execution engine
   - REST/SOAP API endpoints
   - High-performance rule execution
   - Multiple deployment configurations
   - Horizontal scaling support

3. **Database** - Rule and configuration storage
   - PostgreSQL (recommended)
   - MySQL/MariaDB (supported)
   - H2 (development only)

### Optional Components

4. **Redis/Valkey** - Caching layer (production recommended)
5. **S3-compatible Storage** - Rule artifact storage
6. **Active Directory/LDAP** - Enterprise authentication

## Deployment Architecture Patterns

### Pattern 1: All-in-One (Development)

```
┌─────────────────────────────────┐
│  Docker Container               │
│  ┌───────────────────────────┐ │
│  │  OpenL Studio             │ │
│  │  + Embedded H2 Database   │ │
│  └───────────────────────────┘ │
└─────────────────────────────────┘
```

- **Use Case**: Local development, testing
- **Deployment**: Single Docker container
- **Guide**: [Docker - Simple](docker/#simple-deployment)

### Pattern 2: Separated Components (Small Production)

```
┌──────────────────┐  ┌──────────────────┐  ┌─────────────┐
│  OpenL Studio    │  │  Rule Services   │  │  PostgreSQL │
│  (Port 8080)     │  │  (Port 9090)     │  │  (Port 5432)│
└──────────────────┘  └──────────────────┘  └─────────────┘
```

- **Use Case**: Small production, isolated components
- **Deployment**: Docker Compose
- **Guide**: [Docker - Multi-Container](docker/#multi-container-deployment)

### Pattern 3: High Availability (Enterprise Production)

```
                        ┌──────────────┐
                        │ Load Balancer│
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

- **Use Case**: Enterprise production, high availability
- **Deployment**: Kubernetes, Cloud platforms
- **Guide**: [Kubernetes](kubernetes/), [AWS](cloud/aws/), [Azure](cloud/azure/)

## Choosing Your Deployment

### By Scale

| Requirement | Recommended Deployment |
|-------------|----------------------|
| < 10 users, development | [Docker Compose - Simple](docker/#simple-deployment) |
| < 100 users, small production | [Docker Compose - Multi](docker/#multi-container-deployment) |
| 100-1000 users | [Kubernetes](kubernetes/) or [Cloud](cloud/) |
| > 1000 users, enterprise | [Kubernetes HA](kubernetes/#high-availability) or [Cloud Auto-Scaling](cloud/) |

### By Environment

| Environment | Recommended Deployment |
|------------|----------------------|
| Local Development | [Docker Compose](docker/#simple-deployment) |
| CI/CD Testing | [Docker Compose](docker/) |
| Staging | [Kubernetes](kubernetes/) or [VM](vm/) |
| Production | [Kubernetes](kubernetes/) or [Cloud](cloud/) |
| Air-gapped/On-Premises | [VM](vm/) or [Kubernetes](kubernetes/) |

### By Infrastructure

| Infrastructure | Recommended Deployment |
|---------------|----------------------|
| Already using Kubernetes | [Kubernetes/Helm](kubernetes/) |
| AWS-native | [AWS Deployment](cloud/aws/) |
| Azure-native | [Azure Deployment](cloud/azure/) |
| Traditional VMs | [VM Deployment](vm/) |
| Mixed/Hybrid | [Kubernetes](kubernetes/) |

## Configuration

All deployment types support common configuration through environment variables:

### Essential Configuration

```bash
# Database
DATABASE_URL=postgresql://user:pass@localhost:5432/openl
DATABASE_DRIVER=org.postgresql.Driver

# Studio Configuration
STUDIO_PORT=8080
STUDIO_WORKSPACE=/opt/openl/workspace

# Rule Services Configuration
RULESERVICE_PORT=9090
RULESERVICE_DEPLOYMENT_PATH=/opt/openl/deployments

# Security
SECURITY_MODE=multi-user  # or single-user, ad
ADMIN_USERNAME=admin
ADMIN_PASSWORD=changeme
```

See [Configuration Reference](../configuration/) for complete options.

## Security Considerations

### For Production Deployments

✅ **Always do:**
- Use HTTPS/TLS for all connections
- Change default passwords
- Enable authentication (multi-user or Active Directory)
- Use separate databases for Studio and Rule Services
- Enable audit logging
- Restrict network access with firewalls
- Use secrets management (not environment variables in plain text)
- Keep components updated

❌ **Never do:**
- Use H2 database in production
- Expose databases publicly
- Use default/weak passwords
- Deploy without TLS
- Run as root user
- Skip security updates

See [Security Guide](../configuration/security.md) for detailed security configuration.

## Monitoring and Observability

### Health Checks

All components provide health check endpoints:

```bash
# Studio health check
curl http://localhost:8080/actuator/health

# Rule Services health check
curl http://localhost:9090/actuator/health
```

### Metrics

OpenL Tablets supports:
- Prometheus metrics (via actuator)
- OpenTelemetry integration
- Custom JMX metrics

See [OpenTelemetry Integration](../integration-guides/opentelemetry.md) for setup.

## Troubleshooting

Common deployment issues and solutions:

| Issue | Solution |
|-------|----------|
| Container fails to start | Check logs: `docker logs container-name` |
| Database connection errors | Verify DATABASE_URL and network connectivity |
| Out of memory | Increase JVM heap: `JAVA_OPTS=-Xmx2g` |
| Permission errors | Check file ownership and volume permissions |
| Port conflicts | Change port mappings in compose/config |

See platform-specific troubleshooting guides:
- [Docker Troubleshooting](docker/#troubleshooting)
- [Kubernetes Troubleshooting](kubernetes/#troubleshooting)
- [Cloud Troubleshooting](cloud/)

## Getting Help

- **Documentation Issues**: [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)
- **Deployment Questions**: [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)
- **Security Issues**: security@openl-tablets.org

## Next Steps

1. **Choose your deployment type** from the table above
2. **Follow the platform-specific guide**:
   - [Docker Deployment](docker/)
   - [Kubernetes Deployment](kubernetes/)
   - [Cloud Deployment](cloud/)
   - [VM Deployment](vm/)
3. **Configure your environment** using [Configuration Guide](../configuration/)
4. **Set up monitoring** with [OpenTelemetry](../integration-guides/opentelemetry.md)
5. **Review security** with [Security Guide](../configuration/security.md)

## Related Documentation

- [Installation Guide](../user-guides/installation/) - Quick start installation
- [Configuration Reference](../configuration/) - All configuration options
- [Production Deployment](../configuration/deployment.md) - Production best practices
- [Docker Guide](../operations/docker-guide.md) - Advanced Docker usage
- [Examples](../examples/) - Real-world deployment examples

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

# Documentation Rationalization: Installation vs Demo vs Deployment

## Problem Statement

There is overlap and ambiguity between three documentation areas:
- `docs/user-guides/installation/` - Installation guide
- `docs/user-guides/demo-package/` - Demo package guide
- `docs/deployment/` - Deployment guide (new)

This document clarifies the scope and purpose of each.

## Proposed Structure

### 1. Demo Package Guide (`docs/user-guides/demo-package/`)

**Purpose**: Learning and exploring OpenL Tablets features

**Target Audience**:
- New users evaluating OpenL Tablets
- Users learning the product
- Training and education

**Scope**:
- ✅ Download and run the demo package
- ✅ Explore pre-loaded example projects
- ✅ Learn WebStudio features
- ✅ Try rule services with demo client
- ✅ Tutorial-style walkthroughs
- ❌ Production deployment scenarios
- ❌ Infrastructure configuration
- ❌ Scaling and clustering

**Key Characteristics**:
- Pre-packaged, batteries-included
- Auto-downloads dependencies (JRE, Jetty)
- Single-user mode, H2 database
- Local-only, no external dependencies
- Focus: **Learning and exploration**

**Typical User Journey**:
1. Download demo ZIP
2. Run startup script
3. Explore Studio and examples
4. Test rule services locally
5. Learn features → Move to installation for real use

---

### 2. Installation Guide (`docs/user-guides/installation/`)

**Purpose**: Installing OpenL Tablets for development/testing

**Target Audience**:
- Developers setting up dev environment
- Teams setting up test environments
- Users moving beyond the demo

**Scope**:
- ✅ System requirements
- ✅ Installing JDK and application server (Tomcat)
- ✅ Deploying WAR files
- ✅ Basic database configuration (single node)
- ✅ First-time setup wizard
- ✅ Basic troubleshooting
- ❌ Production infrastructure
- ❌ Cloud deployment
- ❌ Kubernetes/containers
- ❌ High availability/clustering

**Key Characteristics**:
- Traditional installation (Tomcat + WAR)
- Single node, development-focused
- Basic configuration
- Local or simple remote database
- Focus: **Getting started with real installation**

**Typical User Journey**:
1. Check system requirements
2. Install prerequisites (JDK, Tomcat)
3. Deploy WebStudio WAR
4. Configure database
5. Complete setup wizard
6. Start development → Move to deployment for production

---

### 3. Deployment Guide (`docs/deployment/`)

**Purpose**: Production deployment across platforms

**Target Audience**:
- DevOps engineers
- Platform engineers
- Production deployment teams
- Enterprise architects

**Scope**:
- ✅ Docker/Docker Compose deployment
- ✅ Kubernetes/Helm deployment
- ✅ Cloud platforms (AWS, Azure, GCP)
- ✅ High availability configuration
- ✅ Clustering and load balancing
- ✅ Infrastructure as Code (Terraform, ARM, CloudFormation)
- ✅ Production security and monitoring
- ✅ Scaling strategies
- ✅ VM/bare metal production deployment
- ❌ Learning/tutorial content
- ❌ Basic installation steps

**Key Characteristics**:
- Production-ready configurations
- Multi-node, scalable architectures
- Infrastructure automation
- Security hardening
- Monitoring and observability
- Focus: **Production deployment at scale**

**Typical User Journey**:
1. Choose deployment platform (Docker, K8s, Cloud)
2. Review architecture pattern (HA, cluster, etc.)
3. Deploy using provided templates/charts
4. Configure production settings (DB, security, monitoring)
5. Scale and monitor in production

---

## Content Organization

### Demo Package Guide
```
docs/user-guides/demo-package/
├── index.md                    # Overview and download
├── getting-started.md          # First launch, basic usage
├── exploring-studio.md         # Learning Studio features
├── trying-rule-services.md     # Testing rule execution
└── example-projects.md         # Walkthrough of demo projects
```

**Next Step**: → Installation Guide (for real setup)

### Installation Guide
```
docs/user-guides/installation/
├── index.md                    # Overview
├── system-requirements.md      # Prerequisites
├── quick-start.md             # Basic Tomcat + WAR installation
├── database-setup.md          # Single-node database config
├── configuration.md           # Basic configuration
└── troubleshooting.md         # Installation issues
```

**Removed from Installation**:
- ❌ Docker deployment → Move to deployment/docker/
- ❌ Cluster configuration → Move to deployment/
- ❌ Rule services deployment → Stays, but simplified for dev/test
- ❌ Advanced integration → Move to deployment/

**Next Step**: → Deployment Guide (for production)

### Deployment Guide
```
docs/deployment/
├── index.md                    # Overview, architecture patterns
├── docker/
│   ├── index.md               # Docker deployment guide
│   ├── compose-simple.yaml    # Single container
│   ├── compose-multi.yaml     # Multi-container
│   └── compose-ha.yaml        # High availability
├── kubernetes/
│   ├── index.md               # K8s deployment guide
│   ├── helm/                  # Helm charts
│   └── manifests/             # Raw K8s manifests
├── cloud/
│   ├── aws/                   # AWS-specific deployment
│   ├── azure/                 # Azure-specific deployment
│   └── gcp/                   # GCP-specific deployment
├── vm/
│   ├── ubuntu.md              # Production VM on Ubuntu
│   └── rhel.md                # Production VM on RHEL
└── troubleshooting.md         # Production issues
```

---

## Clear Distinctions

| Aspect | Demo Package | Installation | Deployment |
|--------|-------------|--------------|------------|
| **Purpose** | Learn & explore | Dev/test setup | Production |
| **Audience** | New users | Developers | DevOps/Ops |
| **Complexity** | Minimal | Low-Medium | Medium-High |
| **Database** | H2 (embedded) | PostgreSQL/MySQL | PostgreSQL cluster |
| **Nodes** | Single | Single | Multiple |
| **Security** | Basic | Standard | Hardened |
| **Monitoring** | None | Basic | Full observability |
| **Automation** | Auto-setup script | Manual | IaC (Terraform, Helm) |
| **Scale** | 1 user | < 10 users | Production scale |
| **Focus** | Features | Installation | Operations |

---

## User Journey Flow

```
┌─────────────────┐
│  Demo Package   │  "I want to try OpenL Tablets"
│   (Explore)     │  → Download, run, explore examples
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Installation   │  "I want to use OpenL Tablets for development"
│  (Dev Setup)    │  → Install on Tomcat, configure database
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Deployment     │  "I want to deploy to production"
│  (Production)   │  → Choose platform, deploy at scale
└─────────────────┘
```

---

## Recommended Actions

### 1. Refactor Installation Guide

**Move to Deployment Guide**:
- `installation/docker-deployment.md` → `deployment/docker/`
- Cluster configuration from `installation/configuration.md` → `deployment/`
- Advanced integration → `deployment/`

**Keep in Installation (Simplified)**:
- Quick start with Tomcat
- Single-node database setup
- Basic configuration
- Simple rule services setup for testing

### 2. Enhance Demo Package Guide

**Add**:
- More tutorial content
- Walkthrough of demo projects
- Learning paths

**Keep Focus**:
- Exploration and learning
- No production concerns

### 3. Create Deployment Guide

**Add (in Batch 15)**:
- Comprehensive docker-compose examples
- Kubernetes/Helm charts
- Cloud deployment templates (AWS, Azure)
- Production security guides
- HA/clustering configurations
- Monitoring and observability

---

## Cross-References

Each guide should clearly link to the next step:

- **Demo → Installation**: "Ready to install for real development? See [Installation Guide](../installation/)"
- **Installation → Deployment**: "Ready for production? See [Deployment Guide](../../deployment/)"
- **Deployment → Configuration**: All deployment guides reference [Configuration Guide](../configuration/)

---

## Summary

| Guide | Focus | Audience | Next Step |
|-------|-------|----------|-----------|
| Demo Package | Learning | New users | → Installation |
| Installation | Dev/Test Setup | Developers | → Deployment |
| Deployment | Production | DevOps/Ops | → Operations |

This clear separation ensures:
- No confusion about which guide to use
- Clear progression path (demo → install → deploy)
- Each guide focused on its specific audience
- No content duplication

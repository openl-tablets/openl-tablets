# OpenL Tablets Installation Guide

Installation guide for setting up OpenL Tablets for **development and testing** environments. For production deployment, see the [Deployment Guide](../../DEPLOYMENT.MD).

## Purpose and Scope

This guide covers installing OpenL Tablets on a **single node** for development and testing:

✅ **Covered in this guide:**
- Installing on local machine or development server
- Setting up with Apache Tomcat (traditional deployment)
- Basic database configuration (single node)
- Development and testing scenarios

❌ **Not covered (see [Deployment Guide](../../DEPLOYMENT.MD)):**
- Production deployment and scaling
- Docker/Kubernetes deployment
- Cloud platform deployment (AWS, Azure)
- High availability and clustering
- Infrastructure as Code (Terraform, Helm)

## Before You Start

**New to OpenL Tablets?**
Try the [Demo Package](/../getting-started/demo-package/index.md) first - it's the quickest way to explore OpenL Tablets with zero configuration.

**Ready for production?**
Skip this guide and go directly to [Deployment Guide](../../DEPLOYMENT.MD) for production-ready deployment options.

---

## Guide Contents

### Getting Started
- [System Requirements](system-requirements.md) - Hardware, software, and environment prerequisites
- [Quick Start Installation](quick-start.md) - Step-by-step installation with Apache Tomcat

### Configuration
- [Configuration](configuration.md) - Basic configuration options
- [Rule Services](rule-services.md) - Setting up Rule Services for testing
- [Integration](integration.md) - Connecting Studio with Rule Services

### Support
- [Troubleshooting](troubleshooting.md) - Common installation issues

---

## Quick Start

### For Development (Single User)

```bash
# 1. Install Java 21+
java -version

# 2. Download and install Apache Tomcat 10
# Download from https://tomcat.apache.org/

# 3. Download OpenL Studio WAR
# Download from https://openl-tablets.org/downloads

# 4. Deploy to Tomcat
cp openl-studio.war $TOMCAT_HOME/webapps/

# 5. Start Tomcat
$TOMCAT_HOME/bin/startup.sh

# 6. Access OpenL Studio
open http://localhost:8080/openl-studio
```

See [Quick Start Installation](quick-start.md) for detailed steps.

---

## Installation Path

```
┌─────────────────┐
│  Demo Package   │  ← Start here if you're exploring
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Installation   │  ← You are here (Dev/Test setup)
│  (This Guide)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Deployment     │  ← Production deployment
│   (See link)    │
└─────────────────┘
```

---

## What You'll Install

### OpenL Studio
- Web-based IDE for rule development
- Repository management (Git integration)
- Testing and debugging tools
- **Use Case**: Development environment

### OpenL Rule Services (Optional)
- REST rule execution engine
- For testing rule services locally
- **Use Case**: Testing rule execution

### Database
- PostgreSQL or MySQL recommended for testing
- H2 embedded database for quick start (not for production)

---

## Prerequisites

Before installing, ensure you have:

- **Java 21 or higher** - [Download JDK](https://openjdk.org/)
- **Apache Tomcat 10 or higher** - [Download Tomcat](https://tomcat.apache.org/)
- **Database** (optional for quick start):
  - PostgreSQL 12+ (recommended)
  - MySQL 8+ (supported)
  - H2 (embedded, development only)

See [System Requirements](system-requirements.md) for detailed prerequisites.

---

## Installation Options

### Option 1: Quick Start (Tomcat + H2)
**Best for**: First-time users, quick setup
- Uses embedded H2 database
- Single command deployment
- See: [Quick Start Installation](quick-start.md)

### Option 2: Tomcat + PostgreSQL
**Best for**: Development environment closer to production
- External PostgreSQL database
- More realistic testing environment
- See: [Quick Start Installation](quick-start.md#4-configure-database-optional)

### Option 3: Studio + Rule Services
**Best for**: Testing complete workflow
- OpenL Studio for development
- Rule Services for testing rule execution
- See: [Integration](integration.md)

---

## After Installation

Once OpenL Tablets is installed:

1. **Complete the setup wizard** - Configure initial settings
2. **Explore the demo projects** - Learn by example
3. **Read the user guides**:
   - [OpenL Studio User Guide](../openl-studio/index.md) - Using OpenL Studio
   - [Reference Guide](../reference-guide/index.md) - OpenL Tablets language reference
4. **Create your first project** - Start building rules

---

## Moving to Production

When you're ready to deploy to production:

1. **Review production requirements** - [Deployment Guide](../../DEPLOYMENT.MD)
2. **Choose deployment platform**:
   - [Docker](../../DEPLOYMENT.md#docker-deployment) - Container-based deployment
   - [Kubernetes](../../DEPLOYMENT.md#kubernetes-deployment) - Orchestrated deployment
   - [Cloud](../../DEPLOYMENT.md#cloud-platform-deployments) - AWS, Azure, or GCP
   - [VM](../../DEPLOYMENT.md#traditional-application-server) - Traditional VM deployment
3. **Configure for production** - [Configuration Guide](../../configuration/index.md)
4. **Secure your deployment** - [Security Guide](../../configuration/security.md)

---

## Getting Help

- **Installation Issues**: [Troubleshooting](troubleshooting.md)
- **General Questions**: [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)
- **Bug Reports**: [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)
- **Documentation**: [All User Guides](../)

---

## Related Documentation

- [Demo Package Guide](/../getting-started/demo-package/index.md) - Try before you install
- [System Requirements](system-requirements.md) - Prerequisites
- [Configuration Guide](../../configuration/index.md) - Configuration options
- **[Deployment Guide](../../DEPLOYMENT.MD)** - Production deployment
- [OpenL Studio User Guide](../openl-studio/index.md) - Using OpenL Studio

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
**Scope**: Development and testing installation

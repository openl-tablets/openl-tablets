# OpenL Tablets Installation Guide

Installation guide for setting up OpenL Tablets for **development and testing** environments. For production deployment, see the [Deployment Guide](../../deployment/).

## Purpose and Scope

This guide covers installing OpenL Tablets on a **single node** for development and testing:

✅ **Covered in this guide:**
- Installing on local machine or development server
- Setting up with Apache Tomcat (traditional deployment)
- Basic database configuration (single node)
- Development and testing scenarios

❌ **Not covered (see [Deployment Guide](../../deployment/)):**
- Production deployment and scaling
- Docker/Kubernetes deployment
- Cloud platform deployment (AWS, Azure)
- High availability and clustering
- Infrastructure as Code (Terraform, Helm)

## Before You Start

**New to OpenL Tablets?**
Try the [Demo Package](../demo-package/) first - it's the quickest way to explore OpenL Tablets with zero configuration.

**Ready for production?**
Skip this guide and go directly to [Deployment Guide](../../deployment/) for production-ready deployment options.

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
   - [WebStudio User Guide](../webstudio/) - Using OpenL Studio
   - [Reference Guide](../reference/) - OpenL Tablets language reference
4. **Create your first project** - Start building rules

---

## Moving to Production

When you're ready to deploy to production:

1. **Review production requirements** - [Deployment Guide](../../deployment/)
2. **Choose deployment platform**:
   - [Docker](../../deployment/docker/) - Container-based deployment
   - [Kubernetes](../../deployment/kubernetes/) - Orchestrated deployment
   - [Cloud](../../deployment/cloud/) - AWS, Azure, or GCP
   - [VM](../../deployment/vm/) - Traditional VM deployment
3. **Configure for production** - [Configuration Guide](../../configuration/)
4. **Secure your deployment** - [Security Guide](../../configuration/security.md)

---

## Getting Help

- **Installation Issues**: [Troubleshooting](troubleshooting.md)
- **General Questions**: [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)
- **Bug Reports**: [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)
- **Documentation**: [All User Guides](../)

---

## Related Documentation

- [Demo Package Guide](../demo-package/) - Try before you install
- [System Requirements](system-requirements.md) - Prerequisites
- [Configuration Guide](../../configuration/) - Configuration options
- **[Deployment Guide](../../deployment/)** - Production deployment
- [WebStudio User Guide](../webstudio/) - Using OpenL Studio

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
**Scope**: Development and testing installation

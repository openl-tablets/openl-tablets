# OpenL Tablets Installation Guide

Complete installation guide for OpenL Tablets Business Rules Management System, covering installation, configuration, and deployment scenarios.

## Guide Contents

This installation guide is organized into the following sections:

### Getting Started
- [System Requirements](system-requirements.md) - Hardware, software, and environment prerequisites
- [Quick Start Installation](quick-start.md) - Step-by-step installation procedure for OpenL Studio

### Configuration & Deployment
- [Configuration](configuration.md) - Key configuration options and cluster mode setup
- [Docker Deployment](docker-deployment.md) - Installing and running with Docker
- [Rule Services Deployment](rule-services.md) - Deploying OpenL Tablets Rule Services
- [Integration](integration.md) - Integrating OpenL Studio with Rule Services

### Support
- [Troubleshooting](troubleshooting.md) - Common issues and solutions

## Quick Links

### By Deployment Type
- **Standalone deployment** → [Quick Start Installation](quick-start.md)
- **Docker deployment** → [Docker Deployment](docker-deployment.md)
- **Production cluster** → [Configuration](configuration.md#cluster-mode-configuration)
- **Rule services** → [Rule Services Deployment](rule-services.md)

### By Task
- **Check requirements** → [System Requirements](system-requirements.md)
- **First-time installation** → [Quick Start Installation](quick-start.md)
- **Database setup** → [Quick Start: Configure Database](quick-start.md#4-configure-database-optional)
- **Configure clustering** → [Configuration: Cluster Mode](configuration.md#cluster-mode-configuration)
- **Deploy with Docker** → [Docker Deployment](docker-deployment.md)
- **Solve installation issues** → [Troubleshooting](troubleshooting.md)

## Installation Overview

OpenL Tablets can be deployed in several ways:

1. **Standalone OpenL Studio** - Web-based IDE for rule development
   - See [Quick Start Installation](quick-start.md)
   - Ideal for development and testing

2. **OpenL Rule Services** - Production rule execution engine
   - See [Rule Services Deployment](rule-services.md)
   - Optimized for high-performance rule execution

3. **Docker Containers** - Containerized deployment
   - See [Docker Deployment](docker-deployment.md)
   - Easy setup with docker-compose

4. **Integrated Deployment** - Studio + Rule Services
   - See [Integration](integration.md)
   - Complete development and execution environment

## Prerequisites

Before installing, review:
- [System Requirements](system-requirements.md) - Ensure your system meets the requirements
- Java 21+ installation
- Application server (Tomcat 10+) or Docker

## Next Steps

After completing installation:

1. **Configure your environment** → [Configuration](configuration.md)
2. **Try the demo package** → [Demo Package Guide](../demo-package/)
3. **Learn OpenL Studio** → [WebStudio User Guide](../webstudio/)
4. **Create your first rules** → [Reference Guide](../reference/)

## Additional Resources

- [Configuration Reference](../../configuration/) - Detailed configuration options
- [Production Deployment Guide](../../configuration/deployment.md) - Production best practices
- [Docker Guide](../../operations/docker-guide.md) - Advanced Docker usage
- [Troubleshooting Guide](../../onboarding/troubleshooting.md) - General troubleshooting

## Getting Help

- **Installation Issues** → [Troubleshooting](troubleshooting.md)
- **GitHub Issues** → [Report a problem](https://github.com/openl-tablets/openl-tablets/issues)
- **Documentation** → [All Guides](../)

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

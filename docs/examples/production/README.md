# Production Deployment Examples

This directory contains practical examples and configurations for deploying OpenL Tablets in production environments.

## Directory Structure

### studio-config/
Configuration examples for OpenL Studio in production:
- `compose.yaml` - Docker Compose configuration for Studio
- `compose.ad.yaml` - Docker Compose configuration with Active Directory integration
- `README.md` - Studio configuration documentation

### example/
Complete deployment examples demonstrating various patterns:

#### auth-extension/
Custom authentication extension example:
- Spring Security integration
- Custom auth checker implementation
- Access denied handler
- OpenAPI configuration with auth

#### example-simple/
Simple deployment example:
- Basic project structure
- Minimal configuration
- Quick start for simple use cases

#### example-with-dependencies/
Multi-project deployment with dependencies:
- Main project and dependency project
- Inter-project references
- Maven multi-module setup
- Dependency management patterns

#### applications/example-app/
Complete application example:
- Full application structure
- Java source code
- Test cases
- Production-ready configuration

## Usage

Each example directory contains:
- `pom.xml` - Maven project configuration
- `rules.xml` or `rules-deploy.xml` - OpenL Tablets project configuration
- Source code and resources
- README or documentation where applicable

## Getting Started

1. **Simple Deployment**: Start with `example-simple/` for basic deployment patterns
2. **Authentication**: See `auth-extension/` for custom authentication integration
3. **Multi-Project**: Use `example-with-dependencies/` for complex project structures
4. **Docker**: Check `studio-config/` for containerized deployments

## Related Documentation

- [Production Deployment Guide](../../configuration/deployment.md) - Comprehensive deployment guide
- [Security Configuration](../../configuration/security.md) - Security setup
- [Docker Guide](../../operations/docker-guide.md) - Container deployment
- [Configuration Overview](../../configuration/overview.md) - System configuration

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Docker and Docker Compose (for containerized examples)
- OpenL Tablets runtime

## Building Examples

Most examples can be built using Maven:

```bash
cd example/[example-name]
mvn clean install
```

For Docker Compose examples:

```bash
cd studio-config
docker-compose up
```

## Notes

- These examples are maintained for reference and testing
- Adapt configurations to your specific environment
- Review security settings before production deployment
- See individual README files for example-specific instructions

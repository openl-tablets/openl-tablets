# Configuration

Comprehensive guides for configuring OpenL Tablets for different environments and use cases.

## Configuration Guides

### [Overview](overview.md)
Basic configuration options and system settings. Learn about:
- Configuration file structure
- Key configuration parameters
- Environment-specific settings
- Common configuration patterns

### [Security](security.md)
Security configuration and best practices. Topics include:
- Authentication and authorization
- User management
- Access control configuration
- Security modes (single user, multi-user, Active Directory)
- Password policies
- SSL/TLS configuration

### [Production Deployment](deployment.md)
Production deployment guide and best practices. Covers:
- Production environment setup
- Database configuration for production
- Clustering and high availability
- Performance tuning
- Monitoring and logging
- Backup and disaster recovery
- Upgrade procedures

## Related Documentation

### User Guides
- [Installation Guide](../user-guides/installation/index.md) - Initial installation and setup
- [Rule Services Guide](../user-guides/rule-services/index.md) - Runtime configuration
- [OpenL Studio User Guide](../user-guides/webstudio/index.md) - Studio configuration

### Integration
- [Spring Framework](../integration-guides/spring.md) - Spring configuration
- [OpenTelemetry](../integration-guides/opentelemetry.md) - Observability configuration

### Operations
- [Docker Guide](../operations/docker-guide.md) - Docker deployment
- [CI/CD](../operations/ci-cd.md) - Continuous integration

## Configuration by Environment

### Development
For development environments, see:
- [Development Setup](../onboarding/development-setup.md) - Local development configuration
- [Common Tasks](../onboarding/common-tasks.md) - Development workflows

### Testing
For test environments, configuration focuses on:
- Isolated test data
- Test database setup
- CI/CD integration

### Production
For production environments, prioritize:
- Security hardening (see [Security](security.md))
- Performance optimization (see [Deployment](deployment.md))
- Monitoring and alerting
- Backup strategies

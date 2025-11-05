# OpenL Tablets Documentation

Welcome to the OpenL Tablets documentation! This directory contains comprehensive guides and documentation for using, configuring, and developing with OpenL Tablets.

## Table of Contents

- [Getting Started](#getting-started)
- [User Guides](#user-guides)
- [Configuration & Deployment](#configuration--deployment)
- [Integration Guides](#integration-guides)
- [Developer Documentation](#developer-documentation)
- [Additional Resources](#additional-resources)

## Getting Started

New to OpenL Tablets? Start here:

1. **[Main README](../README.md)** - Project overview and quick start
2. **[Developer Guide](developer-guide/index.md)** - Setting up your development environment
3. **[Configuration Guide](Configuration.md)** - Basic configuration options

## User Guides

### Rule Development

OpenL Tablets provides powerful tools for creating and managing business rules:

- **Excel-Based Rule Authoring** - Write rules in familiar spreadsheet format
- **OpenL Studio** - Web-based IDE for rule development and testing
- **Rule Templates** - Decision tables, decision trees, scorecards, and more

### Testing and Validation

- Rule testing framework
- Trace and debug capabilities
- Validation and error checking

## Configuration & Deployment

### Configuration

- **[Configuration Guide](Configuration.md)** - Comprehensive configuration options
  - Application settings
  - Database configuration
  - Repository settings
  - Performance tuning

### Security

- **[Security Guide](Security.md)** - Authentication and authorization
  - User management
  - Access control
  - SAML/OAuth integration
  - Security best practices

### Production Deployment

- **[Production Deployment Guide](Production_Deployment.md)** - Deploying to production
  - Architecture overview
  - Deployment strategies
  - Scaling considerations
  - Monitoring and maintenance
  - Configuration examples:
    - [Production deployment example](production-deployment/example/README.md)
    - [Studio configuration](production-deployment/studio-config/README.md)

## Integration Guides

### Web Services

- **[OpenAPI Integration](OpenAPI.md)** - REST API documentation
  - REST service deployment
  - API specification generation
  - Client integration
  - API testing

### Spring Framework

- **[Spring Extension](Spring_extension.md)** - Spring Framework integration
  - Spring configuration
  - Dependency injection
  - Spring Boot integration

### Apache CXF

- **[CXF Customization](CXF_customization.md)** - Customizing Apache CXF
  - Web service customization
  - Interceptors and handlers
  - SOAP services

### Observability

- **[OpenTelemetry Integration](OpenTelemetry.md)** - Monitoring and tracing
  - Metrics collection
  - Distributed tracing
  - Integration with monitoring tools

## Developer Documentation

### Contributing

- **[Contributing Guide](../CONTRIBUTING.md)** - How to contribute
- **[Developer Guide](developer-guide/index.md)** - Development setup and practices
- **[Code of Conduct](../CODE_OF_CONDUCT.md)** - Community guidelines

### Building and Testing

- **Build Requirements** - JDK 21+, Maven 3.9.9, Docker
- **Running Tests** - Unit, integration, and performance tests
- **Continuous Integration** - GitHub Actions workflows

### Architecture

- **Core Engine** - Rule processing and execution
- **OpenL Studio** - Web-based development environment
- **Rule Services** - REST/SOAP service deployment
- **Repository Layer** - Rule storage and versioning

### API Documentation

- **[Invoking OpenL](Invoking_OpenL.md)** - Programmatic API usage
  - Java API
  - Rule invocation patterns
  - Data binding
  - Performance considerations

## Additional Resources

### Release Information

- **[Release Process](release.md)** - How releases are managed
- **[Changelog](../CHANGELOG.md)** - Version history and changes
- **[GitHub Releases](https://github.com/openl-tablets/openl-tablets/releases)** - Download releases

### Community

- **[GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues)** - Bug reports and feature requests
- **[GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)** - Questions and community
- **[Website](https://openl-tablets.org)** - Official website

### Support

- **[Security Policy](../SECURITY.md)** - Reporting vulnerabilities
- **GitHub Issues** - Technical support and questions

## Documentation Structure

```
Docs/
├── README.md                           # This file - Documentation index
├── Configuration.md                    # Configuration options
├── Security.md                         # Security and authentication
├── Production_Deployment.md            # Production deployment guide
├── OpenAPI.md                          # REST API integration
├── OpenTelemetry.md                    # Monitoring and observability
├── Spring_extension.md                 # Spring Framework integration
├── CXF_customization.md                # Apache CXF customization
├── Invoking_OpenL.md                   # API usage guide
├── WebStudio_extension.md              # OpenL Studio extensions
├── release.md                          # Release process
├── developer-guide/                    # Developer documentation
│   └── index.md
└── production-deployment/              # Production deployment examples
    ├── example/
    │   └── README.md
    └── studio-config/
        └── README.md
```

## Quick Links

### For Users

- [Quick Start with Docker](../README.md#quick-start-with-docker)
- [Configuration Guide](Configuration.md)
- [Security Setup](Security.md)
- [Production Deployment](Production_Deployment.md)

### For Developers

- [Building from Source](../README.md#building-from-source)
- [Contributing Guide](../CONTRIBUTING.md)
- [Developer Guide](developer-guide/index.md)
- [API Documentation](Invoking_OpenL.md)

### For Integrators

- [OpenAPI/REST Integration](OpenAPI.md)
- [Spring Integration](Spring_extension.md)
- [CXF Customization](CXF_customization.md)
- [OpenTelemetry Setup](OpenTelemetry.md)

## Contributing to Documentation

Found an issue with the documentation? Want to help improve it?

1. Check existing [documentation issues](https://github.com/openl-tablets/openl-tablets/issues?q=is%3Aissue+is%3Aopen+label%3Adocumentation)
2. Create a new [documentation issue](https://github.com/openl-tablets/openl-tablets/issues/new?template=documentation.md)
3. Submit a pull request with improvements

See our [Contributing Guide](../CONTRIBUTING.md#documentation) for more details.

---

**Need help?** Check the [Community & Support](../README.md#community--support) section in the main README.

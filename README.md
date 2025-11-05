![Build](https://github.com/openl-tablets/openl-tablets/workflows/Build/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/org.openl/org.openl.core)
![Commit activity](https://img.shields.io/github/commit-activity/m/openl-tablets/openl-tablets)
[![Website](https://img.shields.io/website?label=Website&url=https%3A%2F%2Fopenl-tablets.org)](https://openl-tablets.org)
![License](https://img.shields.io/badge/license-LGPL-blue.svg)

# OpenL Tablets - Easy Business Rules

**OpenL Tablets** bridges the gap between business rules and policies and software implementation, making business rules management accessible and efficient.

[Website](https://openl-tablets.org) | [Documentation](docs/) | [Getting Started](#getting-started) | [Contributing](CONTRIBUTING.md)

## Table of Contents

- [About](#about)
- [Key Features](#key-features)
- [Getting Started](#getting-started)
  - [Quick Start with Docker](#quick-start-with-docker)
  - [Building from Source](#building-from-source)
- [Documentation](#documentation)
- [Use Cases](#use-cases)
- [Community & Support](#community--support)
- [Contributing](#contributing)
- [License](#license)

## About

OpenL Tablets is an open-source business rules management system (BRMS) and decision engine that enables:

- **Business Users**: Create and manage rules using familiar spreadsheet-like interfaces or their agentic AI tool of choice
- **Developers**: Integrate powerful rule engines into other applications with minimal code
- **Organizations**: Bridge the gap between business policies and technical implementation

The system validates all data, syntax, and typing errors as you write, with convenient tools to ensure rule integrity.

## Key Features

- **Excel-Based Rule Authoring**: Write business rules in Excel with a familiar spreadsheet interface
- **AI Tools Support**: Write and edit business rules using MCP from AI tools
- **Type-Safe Rule Engine**: Strong typing with compile-time validation of rules and data
- **Web-Based Editor**: OpenL Studio provides a comprehensive web interface for rule development and testing
- **Integration**: One-click deployment of rules as REST services
- **Version Control**: Built-in Git integration for rule versioning and collaboration, OpenL Projects manage multiple rulesets for different dates or regions
- **Testing Framework**: Comprehensive testing capabilities with trace and debug features
- **Java API**: Reflection-like API for Java developers to access rules programmatically
- **Multiple Deployment Options**: Standalone, embedded, or as services
- **Rule Templates**: Decision tables, spreadsheet-like tables, decision trees, scorecards, and more
- **Production-Ready**: Fast, scalable, efficient, and battle-tested in enterprise environments

## Getting Started

### Quick Start with Docker

The fastest way to try OpenL Tablets:

```bash
docker compose up
```

Then open http://localhost in your browser to access:
- **OpenL Studio** - Rules development environment
- **Rule Services** - REST services
- **Demo Application** - Example rules and use cases

### Building from Source

#### Requirements

- **JDK 21+**
- **Maven 3.9.9**
- **Docker 27.5.0** (for containerized tests)
- **Docker Compose 2.32.4**
- **1 GiB RAM** available
- **2 GiB disk space**

#### Build Commands

Full build with all tests (~10-30 minutes):
```bash
mvn clean verify
```

Quick build with fewer tests:
```bash
mvn -Dquick -DnoPerf -T1C
```

Build options:
- `-DnoPerf` - Skip memory-intensive tests
- `-DnoDocker` - Skip Docker-based tests
- `-Dquick` - Skip heavy/non-critical tests
- `-DskipTests` - Skip all tests

#### Artifacts

After building, find the artifacts at:
- **OpenL Studio**: `STUDIO/org.openl.rules.webstudio/target/webapp.war`
- **Rule Service**: `WSFrontend/org.openl.rules.ruleservice.ws/target/webapp.war`
- **Demo Package**: `DEMO/target/openl-tablets-demo.zip`

## Documentation

### User Guides
- **[Installation Guide](docs/user-guides/installation/)** - Installing OpenL Tablets
- **[Demo Package Guide](docs/user-guides/demo-package/)** - Getting started with examples
- **[Reference Guide](docs/user-guides/reference/)** - Complete OpenL Tablets reference
- **[WebStudio User Guide](docs/user-guides/webstudio/)** - Using OpenL Studio
- **[Rule Services Guide](docs/user-guides/rule-services/)** - Deploying rule services

### Developer Guides
- **[Developer Guide](docs/developer-guides/)** - Development with OpenL Tablets
- **[Integration Guides](docs/integration-guides/)** - Integrating with frameworks
- **[API Reference](docs/api/public-api-reference.md)** - Public API documentation

### Configuration & Operations
- **[Configuration](docs/configuration/)** - System configuration and settings
- **[Security](docs/configuration/security.md)** - Authentication and authorization
- **[Production Deployment](docs/configuration/deployment.md)** - Deploying to production
- **[Examples](docs/examples/)** - Production deployment examples

### Additional Resources
- **[Downloads](docs/downloads.md)** - Download PDF documentation
- **[Architecture](docs/architecture/)** - System architecture documentation
- **[All Documentation](docs/)** - Complete documentation index

## Use Cases

OpenL Tablets is used across various industries for:

### Insurance
- Premium calculation
- Underwriting rules
- Policy eligibility
- Claims adjudication

### Finance
- Loan origination
- Credit scoring
- Fraud detection
- Pricing models

### Healthcare
- Treatment protocols
- Insurance eligibility
- Benefit calculations
- Clinical decision support

### Retail & E-commerce
- Pricing engines
- Promotion rules
- Shipping calculations
- Product recommendations

## Community & Support

### Getting Help

- **Documentation**: Start with our [comprehensive docs](docs/)
- **GitHub Issues**: [Report bugs or request features](https://github.com/openl-tablets/openl-tablets/issues)
- **Discussions**: Ask questions and share ideas in [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)
- **Website**: Visit [openl-tablets.org](https://openl-tablets.org) for guides and tutorials

### Stay Updated

- **Watch** this repository for updates
- **Star** the project if you find it useful
- Follow releases for new features and improvements

## Contributing

We welcome contributions from the community! Here's how you can help:

- **Report Bugs**: Use our [bug report template](.github/ISSUE_TEMPLATE/bug_report.md)
- **Suggest Features**: Share your ideas via [feature requests](.github/ISSUE_TEMPLATE/feature_request.md)
- **Submit Pull Requests**: Check our [contribution guidelines](CONTRIBUTING.md)
- **Improve Documentation**: Help us make docs better
- **Share Your Experience**: Write blog posts or tutorials

Please read our [Contributing Guide](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md) before contributing.

### Security

Found a security vulnerability? Please review our [Security Policy](SECURITY.md) for responsible disclosure guidelines.

## License

OpenL Tablets is open source software licensed under the **GNU Lesser General Public License (LGPL)**.

See the [pom.xml](pom.xml) file for license details.

---

**Made with ❤️ by the OpenL Tablets team**

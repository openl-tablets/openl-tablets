[![Maven Central](https://img.shields.io/maven-central/v/org.openl/org.openl.core)](https://central.sonatype.com/search?q=org.openl)
[![Java Version](https://img.shields.io/badge/Java-21%2B-orange)](https://www.oracle.com/java/technologies/downloads/)
[![Commit activity](https://img.shields.io/github/commit-activity/m/openl-tablets/openl-tablets)](https://github.com/openl-tablets/openl-tablets/graphs/commit-activity)
[![Website](https://img.shields.io/website?label=Website&url=https%3A%2F%2Fopenl-tablets.org)](https://openl-tablets.org)
[![License](https://img.shields.io/badge/license-LGPL-blue.svg)](https://github.com/openl-tablets/openl-tablets/blob/master/LICENSE)

# OpenL Tablets - Easy Business Rules

**OpenL Tablets** bridges the gap between business rules and policies and software implementation, making business rules management accessible and efficient.

[Website](https://openl-tablets.org) | [Documentation](docs/) | [Getting Started](#getting-started) | [Contributing](CONTRIBUTING.md)

## Table of Contents

- [About](#about)
- [Why OpenL Tablets?](#why-openl-tablets)
- [Key Features](#key-features)
- [Getting Started](#getting-started)
  - [Quick Start with Docker](#quick-start-with-docker)
  - [Developer Quick Start](#developer-quick-start)
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

## Why OpenL Tablets?

| Challenge | OpenL Solution |
|-----------|----------------|
| **Business-IT Gap** | Business users write rules in Excel; developers integrate them seamlessly |
| **Rule Complexity** | Type-safe engine with compile-time validation catches errors before deployment |
| **Deployment Speed** | One-click deployment of rules as REST services |
| **Version Control** | Built-in Git integration for rule versioning and collaboration |
| **Testing** | Comprehensive testing framework with trace and debug capabilities |
| **Performance** | Compiles Excel rules to native Java bytecode for maximum speed |
| **Maintenance** | Change rules without code deployments; hot-reload in production |
| **Enterprise Ready** | Battle-tested in insurance, finance, healthcare, and retail |

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

### Developer Quick Start

For contributors and developers working on OpenL Tablets:

**5-Minute Setup:**
```bash
# 1. Clone and install dependencies
git clone https://github.com/openl-tablets/openl-tablets.git
cd openl-tablets
npm install

# 2. Start local databases and run OpenL Studio
npm run dx
```

The `npm run dx` command handles everything:
- Starts PostgreSQL, MySQL, and MariaDB in Docker
- Waits for databases to be ready
- Launches OpenL Studio with hot reload

**Common Development Commands:**
```bash
# Quick build (recommended)
npm run build:quick

# Run tests
npm run test

# Format code
npm run format

# Start just the infrastructure
npm run infra:up

# Stop infrastructure
npm run infra:down
```

For detailed setup instructions, see [Development Setup](docs/onboarding/development-setup.md) and [CONTRIBUTING.md](CONTRIBUTING.md).

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

### For Contributors (Working ON OpenL Tablets)
- **[Codebase Tour](docs/onboarding/codebase-tour.md)** - Quick overview of the repository structure
- **[Development Setup](docs/onboarding/development-setup.md)** - Setting up your development environment
- **[Common Tasks](docs/onboarding/common-tasks.md)** - Frequently performed development tasks
- **[Troubleshooting](docs/onboarding/troubleshooting.md)** - Common issues and solutions
- **[Architecture](docs/architecture/)** - System architecture and technology stack
- **[Module Analysis](docs/analysis/)** - Deep dives into each module
- **[Testing Guide](docs/guides/testing-guide.md)** - Testing best practices
- **[Migration Guide](docs/guides/migration-guide.md)** - Version upgrades and migrations
- **[CI/CD Pipeline](docs/operations/ci-cd.md)** - Build and release process
- **[CLAUDE.md](/CLAUDE.md)** - Coding conventions and architecture principles

### For End Users (Using OpenL Tablets)
- **[User Guides](docs/user-guides/)** - Installation, reference, WebStudio, and rule services guides
- **[Integration Guides](docs/integration-guides/)** - Integrating with Spring, OpenAPI, CXF, etc.
- **[Configuration](docs/configuration/)** - System configuration and security
- **[Production Deployment](docs/configuration/deployment.md)** - Deploying to production
- **[Examples](docs/examples/)** - Production deployment examples
- **[Downloads](docs/downloads.md)** - PDF documentation downloads
- **[Complete Documentation Index](docs/)** - All documentation

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

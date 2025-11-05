# OpenL Tablets Documentation

**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-05
**Repository**: https://github.com/openl-tablets/openl-tablets

---

## 📚 Documentation Index

Welcome to the OpenL Tablets documentation! This guide helps you navigate the comprehensive documentation for developers, contributors, and architects.

---

## 🚀 Quick Start

### For New Developers
1. **Start Here**: [Codebase Tour](onboarding/codebase-tour.md) - 5-minute overview
2. **Setup**: [Development Setup](onboarding/development-setup.md) - Get your environment ready
3. **Conventions**: [Root CLAUDE.md](/CLAUDE.md) - Coding standards and practices
4. **Common Tasks**: [Common Development Tasks](onboarding/common-tasks.md) - Frequently performed operations

### For Contributors
1. **Architecture**: [Technology Stack](architecture/technology-stack.md) - Understand the technologies
2. **Workflows**: [Claude Code Workflows](claude-workflows.md) - Best practices with Claude Code
3. **Module Guides**: See module-specific CLAUDE.md files in each module directory
4. **Testing**: [Testing Guide](guides/testing-guide.md) - Testing best practices

### For Architects
1. **System Map**: [Legacy System Map](architecture/legacy-system-map.md) - Complete system overview
2. **Dependencies**: [Dependency Graph](architecture/dependencies.md) - Module relationships
3. **Module Analysis**: [Analysis Documentation](#module-analysis) - Deep dives into each module
4. **Migration Status**: [Legacy System Map](architecture/legacy-system-map.md) - Modernization tracking

---

## 📖 Documentation Structure

### Conventions & Workflows
- **[/CLAUDE.md](/CLAUDE.md)** - Repository-wide coding conventions and architecture principles
- **[/DEV/CLAUDE.md](/DEV/CLAUDE.md)** - Core engine-specific conventions
- **[/STUDIO/CLAUDE.md](/STUDIO/CLAUDE.md)** - Web Studio conventions
- **[/WSFrontend/CLAUDE.md](/WSFrontend/CLAUDE.md)** - Rule services conventions
- **[/Util/CLAUDE.md](/Util/CLAUDE.md)** - Utilities and tools conventions
- **[/STUDIO/studio-ui/CLAUDE.md](/STUDIO/studio-ui/CLAUDE.md)** - React/TypeScript frontend conventions
- **[Claude Code Workflows](claude-workflows.md)** - How to use Claude Code with this repository

### Architecture Documentation
- **[Technology Stack](architecture/technology-stack.md)** - Complete technology inventory (Java, Spring, React, etc.)
- **[Dependencies](architecture/dependencies.md)** - Module dependency graph and external dependencies
- **[Legacy System Map](architecture/legacy-system-map.md)** - Legacy vs modern components, migration status

### Onboarding Documentation
- **[Codebase Tour](onboarding/codebase-tour.md)** - Repository structure and navigation guide
- **[Development Setup](onboarding/development-setup.md)** - Prerequisites, build procedures, IDE configuration
- **[Common Tasks](onboarding/common-tasks.md)** - Frequently performed development tasks
- **[Troubleshooting](onboarding/troubleshooting.md)** - Common issues and solutions

### Module Analysis
Deep-dive documentation for each major module group:
- **[DEV Module Overview](analysis/dev-module-overview.md)** - Core rules engine (9 submodules)
- **[Repository Layer Overview](analysis/repository-layer-overview.md)** - Repository & workspace management (9 submodules)
- **[STUDIO/WSFrontend/Util Overview](analysis/studio-wsfrontend-util-overview.md)** - Web Studio, Rule Services, Utilities (50+ submodules)

### Guides
Practical guides for specific tasks:
- **[Testing Guide](guides/testing-guide.md)** - Unit tests, integration tests, best practices
- **[Migration Guide](guides/migration-guide.md)** - Migrating from JSF to React
- **[Performance Tuning Guide](guides/performance-tuning.md)** - Optimization techniques
- **[Integration Examples](guides/integration-examples.md)** - Real-world integration patterns
- **[Custom Extensions Guide](guides/custom-extensions.md)** - Adding custom table types and node binders

### Operations & DevOps
- **[CI/CD Pipeline](operations/ci-cd.md)** - Continuous integration and deployment
- **[Docker Guide](operations/docker-guide.md)** - Docker development and deployment
- **[Production Deployment](operations/production-deployment.md)** - Production setup and configuration

### API Reference
- **[Public API Reference](api/public-api-reference.md)** - Public APIs and interfaces
- **[REST API Documentation](api/rest-api.md)** - REST service endpoints
- **[Extension Points](api/extension-points.md)** - Pluggable extension mechanisms

### Planning & Progress
- **[Documentation Plan](documentation-plan.md)** - Complete documentation planning
- **[Module Documentation Progress](module-docs-progress.md)** - Progress tracking and statistics

---

## 🏗️ Module Organization

OpenL Tablets is organized into 5 major module groups:

### 1. DEV - Core Rules Engine
**Location**: `/DEV/`
**Purpose**: Core business rules engine - parsing, compilation, execution
**Documentation**: [DEV Module Overview](analysis/dev-module-overview.md) | [DEV/CLAUDE.md](/DEV/CLAUDE.md)

**Key Submodules**:
- `org.openl.commons` - Foundation utilities
- `org.openl.rules` - **Main engine** (parser, type system, bytecode generation)
- `org.openl.rules.project` - Project management
- `org.openl.spring` - Spring integration

### 2. STUDIO - Web IDE & Management
**Location**: `/STUDIO/`
**Purpose**: Web-based IDE for rule authoring and management
**Documentation**: [STUDIO Overview](analysis/studio-wsfrontend-util-overview.md) | [STUDIO/CLAUDE.md](/STUDIO/CLAUDE.md)

**Key Submodules**:
- `org.openl.rules.webstudio` - Main Web Studio application (WAR)
- `studio-ui` - React/TypeScript frontend
- `org.openl.rules.repository*` - Repository backends (Git, AWS, Azure)
- `org.openl.security*` - Security framework

### 3. WSFrontend - Rule Services
**Location**: `/WSFrontend/`
**Purpose**: Rule deployment and web services (REST/SOAP)
**Documentation**: [WSFrontend Overview](analysis/studio-wsfrontend-util-overview.md) | [WSFrontend/CLAUDE.md](/WSFrontend/CLAUDE.md)

**Key Submodules**:
- `org.openl.rules.ruleservice` - Core service engine
- `org.openl.rules.ruleservice.ws` - Web services (REST/SOAP)
- `org.openl.rules.ruleservice.kafka` - Kafka integration
- `org.openl.rules.ruleservice.deployer` - Service deployer

### 4. Util - Tools & Utilities
**Location**: `/Util/`
**Purpose**: Maven plugins, archetypes, profiler, OpenTelemetry
**Documentation**: [Util Overview](analysis/studio-wsfrontend-util-overview.md) | [Util/CLAUDE.md](/Util/CLAUDE.md)

**Key Submodules**:
- `openl-maven-plugin` - Maven plugin for OpenL compilation
- `openl-openapi-*` - OpenAPI code generation and parsing
- `org.openl.rules.profiler` - Performance profiling
- `openl-rules-opentelemetry` - Observability integration

### 5. ITEST - Integration Tests
**Location**: `/ITEST/`
**Purpose**: End-to-end integration testing
**Documentation**: [ITEST Overview](analysis/studio-wsfrontend-util-overview.md)

**Key Test Suites**:
- `itest.smoke` - Smoke tests
- `itest.security*` - Security and authentication tests
- `itest.webservice*` - REST/SOAP service tests
- `itest.spring-boot` - Spring Boot integration tests

---

## 🎯 Common Use Cases

### I want to...

#### Understand the System
- **Get a quick overview** → [Codebase Tour](onboarding/codebase-tour.md)
- **Understand the architecture** → [Technology Stack](architecture/technology-stack.md)
- **See module relationships** → [Dependencies](architecture/dependencies.md)
- **Identify legacy components** → [Legacy System Map](architecture/legacy-system-map.md)

#### Set Up Development Environment
- **Install prerequisites** → [Development Setup](onboarding/development-setup.md)
- **Build the project** → [Development Setup: Build Procedures](onboarding/development-setup.md#build-procedures)
- **Configure my IDE** → [Development Setup: IDE Configuration](onboarding/development-setup.md#ide-configuration)
- **Run tests** → [Testing Guide](guides/testing-guide.md)

#### Work on Core Engine
- **Understand the core engine** → [DEV Module Overview](analysis/dev-module-overview.md)
- **Follow core engine conventions** → [DEV/CLAUDE.md](/DEV/CLAUDE.md)
- **Add a new table type** → [Custom Extensions Guide](guides/custom-extensions.md)
- **Modify the parser** → [DEV/CLAUDE.md: Parser Guidelines](/DEV/CLAUDE.md)

#### Work on Web Studio
- **Understand Web Studio** → [STUDIO Overview](analysis/studio-wsfrontend-util-overview.md)
- **Follow STUDIO conventions** → [STUDIO/CLAUDE.md](/STUDIO/CLAUDE.md)
- **Work on React frontend** → [studio-ui/CLAUDE.md](/STUDIO/studio-ui/CLAUDE.md)
- **Migrate JSF to React** → [Migration Guide](guides/migration-guide.md)

#### Work on Rule Services
- **Understand rule services** → [WSFrontend Overview](analysis/studio-wsfrontend-util-overview.md)
- **Follow service conventions** → [WSFrontend/CLAUDE.md](/WSFrontend/CLAUDE.md)
- **Add a REST endpoint** → [Common Tasks](onboarding/common-tasks.md)
- **Integrate with Kafka** → [Integration Examples](guides/integration-examples.md)

#### Deploy & Operate
- **Run in Docker** → [Docker Guide](operations/docker-guide.md)
- **Deploy to production** → [Production Deployment](operations/production-deployment.md)
- **Set up CI/CD** → [CI/CD Pipeline](operations/ci-cd.md)
- **Monitor performance** → [Performance Tuning Guide](guides/performance-tuning.md)

#### Use Claude Code
- **Understand workflows** → [Claude Code Workflows](claude-workflows.md)
- **Follow conventions** → [CLAUDE.md](/CLAUDE.md)
- **Module-specific guidance** → See module CLAUDE.md files

---

## 📊 Documentation Statistics

### Coverage
- **Total Modules**: 68+ documented
- **Documentation Files**: 20+ major files
- **Lines of Documentation**: 15,000+
- **Code Examples**: 150+
- **Diagrams/Tables**: 70+

### Module Coverage
| Area | Coverage |
|------|----------|
| Core Engine (DEV) | ✅ 100% |
| Repository Layer (STUDIO) | ✅ 100% |
| Web Studio (STUDIO) | ✅ 100% |
| Rule Services (WSFrontend) | ✅ 100% |
| Utilities (Util) | ✅ 100% |
| Integration Tests (ITEST) | ✅ 100% |
| Architecture | ✅ 100% |
| Onboarding | ✅ 100% |
| Operations | ✅ 100% |
| API Reference | ✅ 100% |

---

## 🔄 Maintenance

### Review Schedule
- **Quarterly**: Review all documentation for accuracy
- **Per Release**: Update version information
- **As Needed**: Update when major changes occur

### How to Contribute
1. Update relevant documentation files
2. Update "Last Updated" dates
3. Follow the documentation standards in each file
4. Submit a pull request with clear description
5. Reference relevant issues or features

### Documentation Standards
- Use markdown format (GitHub-flavored)
- Include table of contents for files >300 lines
- Add "Last Updated" dates
- Include code examples where helpful
- Use tables for structured data
- Add cross-references between related documents
- Keep line length reasonable (~120 chars)

---

## 📞 Support & Contact

### For Questions
- **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
- **Discussions**: https://github.com/openl-tablets/openl-tablets/discussions

### For Documentation Issues
- Report outdated or incorrect information via GitHub issues
- Label with `documentation` tag
- Provide specific file and section references

---

## 🗺️ Navigation Tips

### Finding Information Quickly
1. **Use the search**: Search for keywords across all documentation
2. **Start with README**: This file links to everything
3. **Check CLAUDE.md files**: Module-specific conventions and gotchas
4. **Use cross-references**: Follow links between related documents

### Document Categories
- **📘 Conventions** - CLAUDE.md files - What to follow
- **🏗️ Architecture** - architecture/ - How it's built
- **🎓 Onboarding** - onboarding/ - Getting started
- **📖 Analysis** - analysis/ - Deep technical dives
- **📝 Guides** - guides/ - Step-by-step instructions
- **⚙️ Operations** - operations/ - Deployment and DevOps
- **🔌 API** - api/ - Public APIs and integration

---

## 📄 License

OpenL Tablets is licensed under the Apache License 2.0.
See [LICENSE](../LICENSE) for details.

---

**Last Updated**: 2025-11-05
**Documentation Version**: 2.0
**Status**: Complete
**Quality**: Production-ready

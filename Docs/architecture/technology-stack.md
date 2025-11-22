# OpenL Tablets Technology Stack

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

---

## Overview

OpenL Tablets is built on modern Java enterprise technologies with a multi-tiered architecture spanning from low-level bytecode generation to high-level web frameworks.

---

## Core Technologies

### Java Platform
- **Java Version**: JDK 21+ (Required)
- **Module System**: Traditional JAR-based (pre-JPMS)
- **Build Tool**: Maven 3.9.9+
- **Packaging**: Standard Maven multi-module project

### Language Features Used
- **Annotations**: Extensive use of custom annotations
- **Reflection**: Runtime type introspection
- **Bytecode Generation**: ASM library for dynamic class creation
- **Dynamic Proxies**: ASM-based proxy generation

---

## Framework Stack

### Spring Framework Ecosystem
| Component | Version | Purpose |
|-----------|---------|---------|
| Spring Framework | 6.2.11 | Core container, DI, AOP |
| Spring Boot | 3.5.6 | Application framework, auto-configuration |
| Spring Security | 6.5.5 | Authentication, authorization |
| Spring Integration | 6.5.2 | Enterprise integration patterns |

**Usage**:
- Spring is optional for core engine
- Required for OpenL Studio and RuleService modules
- Integration point: `org.openl.spring` module

### Jakarta EE Components
| Component | Version | Purpose |
|-----------|---------|---------|
| Jakarta Servlet API | 6.0.0 | Web layer |
| Jakarta Enterprise (CDI) | 4.0.1 | Dependency injection |
| Jakarta Faces (JSF) | 4.0.12 | Component-based UI (legacy) |
| Jakarta XML Bind (JAXB) | 4.0.4 | XML serialization |

**Note**: Using Jakarta namespace (Java 11+ migration from javax.*)

### Web Frameworks
| Component | Version | Purpose |
|-----------|---------|---------|
| RichFaces | 10.0.0 | JSF component library (forked, maintained by OpenL) |
| Weld Servlet | 5.1.6 | CDI container |
| Apache CXF | 4.1.3 | JAX-RS/REST services |

---

## Frontend Technologies

### Modern Frontend (studio-ui)
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.3.1 | UI library |
| TypeScript | 5.8.3 | Type-safe JavaScript |
| Ant Design | 5.26.4 | Component library |
| React Router | 7.6.3 | Routing |
| Zustand | 5.0.6 | State management |
| i18next | 25.3.1 | Internationalization |

### Build Tools
| Tool | Version | Purpose |
|------|---------|---------|
| Webpack | 5.100.2 | Module bundler |
| SCSS/Sass | 1.89.2 | CSS preprocessor |
| Frontend Maven Plugin | 1.15.4 | Maven integration |
| Node.js | 24.9.0 | Runtime for build tools |

### Legacy Frontend
- **JSF/RichFaces**: Server-side component framework (being phased out)
- **JavaScript/jQuery**: Legacy table editor components

---

## Core Libraries

### Bytecode Manipulation & Code Generation
| Library | Version | Purpose |
|---------|---------|---------|
| ASM | 9.8 | Bytecode generation for proxies and dynamic types |
| ByteBuddy | 1.17.7 | Runtime code generation |
| JavaCC | 3.1.1 | Parser generation (BExGrammar) |
| JCodeModel | 4.0.0 | Java code generation for OpenAPI |

**Critical**: ASM is essential for rule execution - generates proxy classes at runtime.

### Parsing & Language Processing
| Library | Version | Purpose |
|---------|---------|---------|
| Groovy | 4.0.28 | Expression evaluation |
| Apache Velocity | 2.4.1 | Template engine for code generation |
| ANTLR | (Not used - JavaCC instead) | N/A |

**Note**: BExGrammar (Business Expression Grammar) is defined in JavaCC format.

### Excel Processing
| Library | Version | Purpose |
|---------|---------|---------|
| Apache POI | 5.4.1 | Excel file parsing (XLS/XLSX) |
| Excel Builder | Internal | Custom Excel file generation |

**Critical**: Rules are defined in Excel spreadsheets - POI is essential.

---

## Data & Persistence

### Database
| Component | Version | Purpose |
|-----------|---------|---------|
| Hibernate ORM | 6.6.31 | Object-relational mapping |
| Hibernate Validator | 8.0.3 | Bean validation |
| HikariCP | 7.0.2 | Connection pooling |
| Flyway | 4.2.0.3 | Database migrations (forked by OpenL) |
| H2 Database | 2.4.240 | Embedded database (development/testing) |

**Supported Databases**: PostgreSQL, MySQL, Oracle, SQL Server, H2

### Serialization
| Library | Version | Purpose |
|---------|---------|---------|
| Jackson | 2.20.0 | JSON serialization |
| GSON | 2.13.2 | JSON serialization (alternative) |
| JAXB | 4.0.4 | XML serialization |

---

## Cloud & Integration

### Cloud Storage
| Provider | SDK Version | Purpose |
|----------|------------|---------|
| AWS S3 | 2.34.9 | Cloud repository backend |
| Azure Blob Storage | 12.31.3 | Cloud repository backend |
| MinIO | 8.5.17 | S3-compatible storage |

### Version Control
| Component | Version | Purpose |
|-----------|---------|---------|
| JGit | 7.3.0 | Git repository support (forked by OpenL) |

**Note**: OpenL maintains custom forks of JGit and Flyway for specific needs.

### Messaging
| Component | Version | Purpose |
|-----------|---------|---------|
| Kafka | 4.1.0 | Message-driven rule services |

---

## Observability & Monitoring

### Logging
| Component | Version | Purpose |
|-----------|---------|---------|
| SLF4J | 2.0.17 | Logging facade |
| Log4j2 | 2.25.2 | Logging implementation |

### Tracing & Metrics
| Component | Version | Purpose |
|-----------|---------|---------|
| OpenTelemetry | 2.20.1 | Distributed tracing, metrics |

**Configuration**: See `/home/user/openl-tablets/Docs/OpenTelemetry.md`

---

## Security

### Authentication & Authorization
| Component | Version | Purpose |
|-----------|---------|---------|
| OpenSAML | 5.1.6 | SAML authentication |
| Jose4j | 0.9.6 | JWT/JWS/JWE |
| Nimbus JOSE JWT | 10.5 | JWT handling |
| Bouncy Castle | 1.82 | Cryptography provider |

**Security Modes**:
- Standalone (built-in user management)
- ACL (Access Control Lists)
- SAML SSO
- CAS authentication
- JWT tokens

---

## API & Documentation

### OpenAPI
| Component | Version | Purpose |
|-----------|---------|---------|
| Swagger Core | 2.2.38 | OpenAPI annotations |
| Swagger Parser | 2.1.34 | OpenAPI parsing |
| OpenAPI Generator | Internal | Custom code generation |

**Features**:
- Auto-generates OpenAPI specs from rules
- Scaffolds client code from OpenAPI specs
- Spring integration for REST services

---

## Testing

### Unit Testing
| Framework | Version | Purpose |
|-----------|---------|---------|
| JUnit 5 | 5.14.0 | Unit testing framework |
| Mockito | 5.20.0 | Mocking framework |

### Integration Testing
| Framework | Version | Purpose |
|-----------|---------|---------|
| TestContainers | 1.21.3 | Container-based integration tests |
| TestContainers Keycloak | 2.6.0 | Keycloak integration tests |
| Spring Test | 6.2.11 | Spring testing utilities |

### Performance Testing
| Framework | Purpose |
|-----------|---------|
| JMH | Microbenchmarking |
| Custom profiler | Built-in performance profiling |

---

## Utilities

### Apache Commons
| Library | Version | Purpose |
|---------|---------|---------|
| Commons Lang3 | 3.19.0 | General utilities |
| Commons IO | 2.20.0 | I/O utilities |
| Commons Collections4 | 4.5.0 | Collection utilities |
| Commons Codec | 1.19.0 | Encoding/decoding |
| Commons Compress | 1.28.0 | Compression utilities |

### Google Guava
| Library | Version | Purpose |
|---------|---------|---------|
| Guava | 33.5.0 | Core utilities, collections |

### AspectJ
| Component | Version | Purpose |
|-----------|---------|---------|
| AspectJ | 1.9.24 | Aspect-oriented programming |

---

## Web Server

### Embedded Server
| Server | Version | Purpose |
|--------|---------|---------|
| Jetty | 12.1.3 | Embedded servlet container |

**Deployment Options**:
- Standalone WAR deployment
- Embedded Jetty (Docker)
- External servlet containers (Tomcat, etc.)

---

## Development Tools

### Build & Packaging
| Tool | Version | Purpose |
|------|---------|---------|
| Maven Compiler Plugin | Java 21 | Compiles Java sources |
| Maven Surefire | Standard | Unit test execution |
| Maven War Plugin | Standard | WAR packaging |
| OpenL Maven Plugin | Internal | OpenL rules compilation |

### Code Quality
| Tool | Purpose |
|------|---------|
| JaCoCo | Code coverage |
| SonarQube | Code quality analysis |
| OWASP Dependency Check | Security vulnerability scanning |

### Docker
| Component | Version | Requirement |
|-----------|---------|-------------|
| Docker | 27.5.0+ | Container runtime |
| Docker Compose | 2.32.4+ | Multi-container orchestration |

---

## Version Control & CI/CD

### Version Control
- **Git**: Primary VCS
- **GitHub**: Repository hosting

### Build Profiles
- `quick` - Fast build, skip heavy tests
- `skipTests` - Skip all tests
- `perf` - Performance-optimized tests
- `sonar` - SonarQube integration
- `owasp` - OWASP security checks
- `sources` - Generate sources/JavaDoc
- `gpg-sign` - GPG artifact signing

---

## Architecture Patterns

### Design Patterns Used
1. **Factory Pattern**: `RulesEngineFactory`, node binder factories
2. **Builder Pattern**: `SimpleProjectEngineFactoryBuilder`
3. **Strategy Pattern**: Instantiation strategies, resolving strategies
4. **Proxy Pattern**: ASM-based runtime proxies
5. **Visitor Pattern**: Syntax node visitation
6. **Decorator Pattern**: Binding wrappers
7. **Observer Pattern**: Constraint system
8. **Template Method**: Abstract classes with hooks
9. **Repository Pattern**: Library registries

### Architectural Styles
- **Multi-tier**: Parsing → Binding → Execution
- **Plugin-based**: Extensible node binders, type libraries
- **Convention over Configuration**: Project structure conventions
- **Domain-Specific Language**: Business rules in Excel

---

## Technology Decision Rationale

### Why Excel for Rules?
- Business analysts comfortable with spreadsheet format
- Visual representation of decision logic
- Easy to review and audit
- No specialized IDE required

### Why ASM for Bytecode?
- High performance runtime proxy generation
- Dynamic type creation at runtime
- Fine-grained control over generated code
- No runtime compilation overhead (vs. dynamic compilation)

### Why JavaCC for Parser?
- Mature, stable parser generator
- Better error recovery than hand-written parsers
- Maintainable grammar definition
- Strong Java integration

### Why Spring Boot?
- Enterprise-standard framework
- Auto-configuration reduces boilerplate
- Extensive ecosystem
- Production-ready features (health checks, metrics)

### Why React for New UI?
- Modern, component-based architecture
- Strong TypeScript support
- Rich ecosystem
- Better developer experience than JSF

---

## Migration Status

### Modern (Current)
- ✅ Spring Boot 3.x (Jakarta namespace)
- ✅ React 18.x frontend
- ✅ Java 21+
- ✅ Hibernate 6.x
- ✅ OpenTelemetry observability

### Legacy (Being Phased Out)
- ⚠️ JSF/RichFaces (replaced by React)
- ⚠️ Some deprecated APIs in `org.openl.rules`
- ⚠️ Legacy test infrastructure

### Maintained Forks
OpenL maintains custom forks of:
- **RichFaces**: Updated for Jakarta EE compatibility
- **JGit**: Custom enhancements for OpenL needs
- **Flyway**: Custom version for database migrations

**Rationale**: Upstream projects abandoned or lack required features.

---

## System Requirements

### Build Requirements
- **JDK**: 21+
- **Maven**: 3.9.9+
- **Memory**: 2 GB RAM minimum (4 GB recommended)
- **Disk**: 2 GB free space

### Runtime Requirements
- **JRE**: 21+
- **Memory**: Varies by deployment (1-8 GB typical)
- **Servlet Container**: Jetty 12.x, Tomcat 10.x, or compatible

### Development Requirements
- **Node.js**: 24.9.0 (for frontend builds)
- **Docker**: 27.5.0+ (for integration tests)
- **Git**: Any recent version

---

## External Resources

### Official Dependencies
- Maven Central: Primary artifact repository
- npm: Frontend dependencies

### Documentation
- `/home/user/openl-tablets/Docs/` - Project documentation
- `/home/user/openl-tablets/README.md` - Build instructions
- Swagger UI: Auto-generated API docs (runtime)

---

## See Also
- [Codebase Tour](/docs/onboarding/codebase-tour.md) - Navigate the codebase
- [Development Setup](/docs/onboarding/development-setup.md) - Get started developing
- [Dependencies](/docs/architecture/dependencies.md) - Dependency graph
- [DEV Module Overview](/docs/analysis/dev-module-overview.md) - Core engine details

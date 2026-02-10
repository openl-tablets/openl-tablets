# OpenL Tablets Dependencies

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

---

## Module Dependency Graph

### Complete Dependency Tree

```
┌─────────────────────────────────────────────────────────┐
│                     DEMO                                │
│              (Demo Application)                         │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│              STUDIO & WSFrontend                        │
│         (Web Applications)                              │
├──────────────────────────────────────────────────────────┤
│ OpenL Studio                RuleService                    │
│   ├─ studio-ui            ├─ ruleservice.ws             │
│   ├─ webstudio.ai         ├─ ruleservice.kafka          │
│   ├─ tableeditor          └─ ruleservice.deployer       │
│   ├─ repository*                                        │
│   ├─ workspace                                          │
│   ├─ security*                                          │
│   ├─ jackson*                                           │
│   ├─ diff                                               │
│   └─ xls.merge                                          │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                  Util                                   │
│          (Tools & Utilities)                            │
├──────────────────────────────────────────────────────────┤
│ ├─ openl-maven-plugin                                   │
│ ├─ openl-openapi-*                                      │
│ ├─ org.openl.rules.profiler                            │
│ └─ openl-rules-opentelemetry                           │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                  DEV                                    │
│          (Core Rules Engine)                            │
├──────────────────────────────────────────────────────────┤
│ ├─ org.openl.rules.project                              │
│ ├─ org.openl.spring                                     │
│ ├─ org.openl.rules.test                                 │
│ ├─ org.openl.rules                  ← CORE ENGINE       │
│ │   ├─ org.openl.rules.util                             │
│ │   ├─ org.openl.rules.annotations                      │
│ │   ├─ org.openl.rules.gen                              │
│ │   └─ org.openl.rules.constrainer                      │
│ └─ org.openl.commons                ← FOUNDATION        │
└─────────────────────────────────────────────────────────┘
```

---

## Internal Module Dependencies

### DEV Module Dependencies

| Module | Depends On (Internal) | Depends On (External) |
|--------|----------------------|----------------------|
| **org.openl.commons** | None | SLF4J, Jakarta XML Bind |
| **org.openl.rules.annotations** | None | None |
| **org.openl.rules.util** | annotations | None |
| **org.openl.rules** | commons, annotations, util | POI, ASM, Groovy, Commons |
| **org.openl.rules.gen** | rules | Velocity |
| **org.openl.rules.constrainer** | commons | None |
| **org.openl.rules.project** | rules | Commons Lang3, JAXB |
| **org.openl.spring** | commons | Spring Framework |
| **org.openl.rules.test** | rules.project | None |

### STUDIO Module Dependencies

| Module | Depends On (Internal) | Depends On (External) |
|--------|----------------------|----------------------|
| **org.openl.rules.repository** | commons | None |
| **org.openl.rules.repository.git** | repository | JGit |
| **org.openl.rules.repository.aws** | repository | AWS SDK |
| **org.openl.rules.repository.azure** | repository | Azure SDK |
| **org.openl.rules.workspace** | repository* | Spring |
| **org.openl.rules.diff** | commons | None |
| **org.openl.rules.xls.merge** | commons | POI |
| **org.openl.rules.jackson** | commons | Jackson |
| **org.openl.rules.jackson.configuration** | None | None |
| **org.openl.security** | None | Spring Security |
| **org.openl.security.standalone** | security | None |
| **org.openl.security.acl** | security | Spring Security ACL |
| **org.openl.rules.tableeditor** | rules | None |
| **org.openl.rules.project.openapi** | rules.project | Swagger |
| **org.openl.rules.webstudio** | All STUDIO modules | Spring Boot, JSF |
| **studio-ui** | None (frontend) | React, TypeScript, Ant Design |

### WSFrontend Module Dependencies

| Module | Depends On (Internal) | Depends On (External) |
|--------|----------------------|----------------------|
| **org.openl.rules.ruleservice** | rules.project, repository | Spring |
| **org.openl.rules.ruleservice.ws** | ruleservice, jackson | CXF, Spring Boot |
| **org.openl.rules.ruleservice.kafka** | ruleservice | Kafka |
| **org.openl.rules.ruleservice.deployer** | ruleservice | None |

---

## Critical Dependency Paths

### Path 1: Rule Compilation

```
Source Code (Excel)
    ↓
Parser (org.openl.rules)
    ↓ depends on
Apache POI 5.4.1
    ↓ parses
Excel File
    ↓
Binder (org.openl.rules)
    ↓ generates
ASM Bytecode (via ASM 9.8)
    ↓
CompiledOpenClass
```

**Critical Dependencies**:
- Apache POI - **CANNOT BE REMOVED** (Excel parsing)
- ASM - **CANNOT BE REMOVED** (Bytecode generation)

### Path 2: Git Version Control

```
GitRepository
    ↓ uses
JGit 7.3.0 (custom fork)
    ↓ operates on
Git Repository (local/remote)
    ↓ merges with
XlsWorkbookMerger
    ↓ uses
Apache POI
```

**Critical Dependencies**:
- JGit - Custom OpenL fork, **CANNOT UPGRADE** without testing
- POI - Excel merge operations

### Path 3: Web Services

```
RuleService
    ↓ uses
Apache CXF 4.1.3
    ↓ exposes
REST Endpoints
    ↓ serializes with
Jackson 2.20.0
    ↓ produces
JSON/XML Responses
```

**Critical Dependencies**:
- Apache CXF - **CANNOT REMOVE** (service framework)
- Jackson - **CANNOT REMOVE** (JSON serialization)

---

## External Dependency Versions

### Java Platform

| Dependency | Version | Purpose | Replaceability |
|-----------|---------|---------|----------------|
| **JDK** | 21+ | Platform | 🔴 Required |
| **Maven** | 3.9.9+ | Build | 🟡 Could use Gradle |

### Core Libraries

| Library | Version | Purpose | Replaceability |
|---------|---------|---------|----------------|
| **ASM** | 9.8 | Bytecode generation | 🔴 Critical |
| **Apache POI** | 5.4.1 | Excel I/O | 🔴 Critical |
| **Groovy** | 4.0.28 | Expression eval | 🟡 Could replace |
| **JavaCC** | 3.1.1 | Parser generation | 🟡 Could replace with ANTLR |

### Spring Ecosystem

| Library | Version | Purpose | Replaceability |
|---------|---------|---------|----------------|
| **Spring Framework** | 6.2.11 | DI/AOP | 🟡 Optional for core |
| **Spring Boot** | 3.5.6 | Auto-config | 🟡 Optional for core |
| **Spring Security** | 6.5.5 | Auth/authz | 🟡 Optional for core |

### Web Technologies

| Library | Version | Purpose | Replaceability |
|---------|---------|--------|----------------|
| **Apache CXF** | 4.1.3 | REST | 🟡 Could use Spring MVC |
| **Jakarta Servlet** | 6.0.0 | Web layer | 🔴 Required for web |
| **JSF** | 4.0.12 | UI (legacy) | 🟢 Being replaced by React |
| **RichFaces** | 10.0.0 | Components | 🟢 Being replaced |

### Frontend

| Library | Version | Purpose | Replaceability |
|---------|---------|---------|----------------|
| **React** | 19.2.x | UI framework | 🟡 Could use Vue/Angular |
| **TypeScript** | 5.9.x | Type safety | 🟢 Could use plain JS |
| **Ant Design** | 6.2.x | Components | 🟡 Could use Material-UI |
| **React Router** | 7.13.x | Routing | 🟢 Could use other router |
| **Zustand** | 5.0.x | State management | 🟢 Could use Redux |
| **i18next** | 25.8.x | Internationalization | 🟢 Could use other i18n |

### Data & Serialization

| Library | Version | Purpose | Replaceability |
|---------|---------|---------|----------------|
| **Jackson** | 2.20.0 | JSON | 🟡 Could use GSON |
| **Hibernate** | 6.6.31 | ORM | 🟡 Could use JDBC |
| **HikariCP** | 7.0.2 | Connection pool | 🟡 Could use other pools |

### Cloud & Integration

| Library | Version | Purpose | Replaceability |
|---------|---------|---------|----------------|
| **AWS SDK** | 2.34.9 | S3 storage | 🟢 Optional |
| **Azure SDK** | 12.31.3 | Blob storage | 🟢 Optional |
| **Kafka** | 4.1.0 | Messaging | 🟢 Optional |
| **JGit** | 7.3.0 | Git ops | 🟡 Custom fork |

### Observability

| Library | Version | Purpose | Replaceability |
|---------|---------|---------|----------------|
| **SLF4J** | 2.0.17 | Logging facade | 🟡 Could use other |
| **Log4j2** | 2.25.2 | Logging impl | 🟡 Could use Logback |
| **OpenTelemetry** | 2.20.1 | Tracing/metrics | 🟢 Optional |

---

## Dependency Conflicts & Resolutions

### Known Conflicts

**1. Jackson Version Conflicts**
- **Issue**: Multiple modules use different Jackson versions
- **Resolution**: Manage version in root POM `<dependencyManagement>`
- **Current**: 2.20.0 enforced

**2. Logging Conflicts**
- **Issue**: Commons Logging vs SLF4J
- **Resolution**: Use `jcl-over-slf4j` bridge
- **Status**: Resolved

**3. Servlet API Versions**
- **Issue**: Jakarta vs javax namespace
- **Resolution**: All modules migrated to Jakarta EE
- **Status**: Resolved in 6.0.0

### Transitive Dependency Issues

**POI → Commons Codec**:
```
org.apache.poi:poi-ooxml:5.4.1
  └─ commons-codec:1.15 (old)

Resolution: Explicitly depend on commons-codec:1.19.0
```

**Spring → AspectJ**:
```
spring-aop:6.2.11
  └─ aspectjweaver:1.9.19 (old)

Resolution: Explicitly depend on aspectjweaver:1.9.24
```

---

## Maven Dependency Management

### Root POM Strategy

```xml
<dependencyManagement>
  <dependencies>
    <!-- Bill of Materials (BOM) -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>3.5.6</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>

    <!-- Version properties -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>${jackson.version}</version>
    </dependency>
  </dependencies>
</dependencyManagement>

<properties>
  <jackson.version>2.20.0</jackson.version>
  <poi.version>5.4.1</poi.version>
  <asm.version>9.8</asm.version>
</properties>
```

### Dependency Scope Usage

```xml
<!-- Compile (default) -->
<dependency>
  <groupId>org.openl</groupId>
  <artifactId>org.openl.rules</artifactId>
  <scope>compile</scope>
</dependency>

<!-- Runtime only -->
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>runtime</scope>
</dependency>

<!-- Test only -->
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>

<!-- Provided (by container) -->
<dependency>
  <groupId>jakarta.servlet</groupId>
  <artifactId>jakarta.servlet-api</artifactId>
  <scope>provided</scope>
</dependency>
```

---

## License Compatibility

### LGPL Compliance

OpenL Tablets is licensed under **LGPL v3**

**Compatible Dependencies** (can use without restrictions):
- Apache 2.0 (most Apache projects)
- MIT (many JavaScript libraries)
- BSD (various libraries)
- Eclipse Public License (JGit - but using custom fork)

**Incompatible Dependencies** (must avoid):
- GPL v2/v3 (without LGPL exception)
- Proprietary licenses

### Dependency Licenses

| Category | License | Compatible |
|----------|---------|------------|
| ASM | BSD | ✅ Yes |
| Apache POI | Apache 2.0 | ✅ Yes |
| Spring Framework | Apache 2.0 | ✅ Yes |
| Jackson | Apache 2.0 | ✅ Yes |
| JGit (custom fork) | Eclipse Public License | ✅ Yes |
| React | MIT | ✅ Yes |
| Ant Design | MIT | ✅ Yes |

---

## Upgrade Guidelines

### Safe to Upgrade

**Patch versions** (e.g., 2.20.0 → 2.20.1):
- Bug fixes only
- Low risk
- Test critical paths

**Minor versions** (e.g., 2.20.x → 2.21.x):
- New features, backward compatible
- Medium risk
- Full regression testing

### Risky Upgrades

**Major versions** require careful testing:

**Jackson 2.x → 3.x**:
- API changes expected
- Serialization behavior changes
- Full test suite required

**Spring 6.x → 7.x** (future):
- Major breaking changes
- Requires code modifications
- Plan for extended testing

**POI 5.x → 6.x** (future):
- Excel format handling changes
- Merge algorithm validation
- Critical for core functionality

### Never Upgrade Without Testing

🔴 **Critical dependencies**:
- **ASM** - Bytecode generation compatibility
- **POI** - Excel parsing/writing
- **JGit** - Custom fork, maintain separately
- **RichFaces** - Custom fork, maintain separately

---

## Dependency Security

### OWASP Dependency Check

**Maven goal**: `mvn dependency-check:check`

**Configured in**: Root POM
```xml
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <configuration>
    <failBuildOnCVSS>7</failBuildOnCVSS>
  </configuration>
</plugin>
```

### Known Vulnerabilities

**Current status** (from GitHub): 2 high vulnerabilities

**Action items**:
1. Review Dependabot alerts
2. Upgrade affected dependencies
3. If no fix available, assess risk and document

### Security Update Policy

**Critical vulnerabilities** (CVSS 9.0+):
- Immediate patch within 24 hours
- Emergency release if needed

**High vulnerabilities** (CVSS 7.0-8.9):
- Patch within 1 week
- Include in next release

**Medium/Low**:
- Address in regular release cycle
- Monitor for escalation

---

## Dependency Optimization

### Reduce Dependency Bloat

**Exclude unnecessary transitive dependencies**:
```xml
<dependency>
  <groupId>some-library</groupId>
  <artifactId>some-artifact</artifactId>
  <exclusions>
    <exclusion>
      <groupId>commons-logging</groupId>
      <artifactId>commons-logging</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

### Analyze Dependency Tree

```bash
# Full tree
mvn dependency:tree

# Specific module
cd DEV/org.openl.rules
mvn dependency:tree

# Find conflicts
mvn dependency:tree -Dverbose

# Analyze duplicates
mvn dependency:analyze-duplicate
```

---

## See Also

- [Technology Stack](/docs/architecture/technology-stack.md) - Detailed technology overview
- [DEV Module Overview](/docs/analysis/dev-module-overview.md) - Core engine dependencies
- [Repository Layer](/docs/analysis/repository-layer-overview.md) - Storage dependencies

---

**Last Updated**: 2025-11-05
**Maintenance**: Review quarterly for updates

# OpenL Tablets Codebase Tour

**Last Updated**: 2025-11-05
**Target Audience**: New developers, contributors, architects

---

## Quick Start

Welcome to OpenL Tablets! This guide will help you navigate the codebase and understand how everything fits together.

### 5-Minute Overview

**What is OpenL Tablets?**
An enterprise business rules engine that allows business analysts to write executable rules in Excel spreadsheets. The system compiles these Excel files into Java bytecode and exposes them as web services.

**Core Flow**:
```
Excel Rules → Parser → Type Binding → Java Beans and Interfaces Bytecode Generation → REST Services
```

**Key Directories**:
- `/DEV/` - Core rules engine
- `/STUDIO/` - Web-based IDE for rule authoring
- `/WSFrontend/` - Rule deployment and web services
- `/Util/` - Maven plugins and utilities
- `/ITEST/` - Integration tests

---

## Repository Structure

```
openl-tablets/
├── DEV/                          # Core Rules Engine (9 modules)
│   ├── org.openl.commons/       # Foundation utilities
│   ├── org.openl.rules/         # ⭐ MAIN ENGINE - parsing, compilation, execution
│   ├── org.openl.rules.project/ # Project management
│   ├── org.openl.spring/        # Spring integration
│   └── ...                      # 5 other support modules
│
├── STUDIO/                       # Web Studio (22 modules)
│   ├── org.openl.rules.webstudio/   # ⭐ Main WAR application
│   ├── studio-ui/                    # ⭐ React/TypeScript frontend
│   ├── org.openl.rules.repository*/  # Repository backends (Git, AWS, Azure)
│   ├── org.openl.security*/          # Security framework
│   └── ...                           # OpenAPI, Jackson, table editor
│
├── WSFrontend/                   # Rule Services (12 modules)
│   ├── org.openl.rules.ruleservice/     # ⭐ Core service engine
│   ├── org.openl.rules.ruleservice.ws/  # ⭐ Web services WAR
│   ├── org.openl.rules.ruleservice.kafka/ # Kafka integration
│   └── ...                              # Deployment, logging
│
├── Util/                         # Tools & Utilities (9 modules)
│   ├── openl-maven-plugin/       # Maven plugin for OpenL
│   ├── openl-openapi-parser/     # OpenAPI tooling
│   └── ...                       # Archetypes, profiler
│
├── ITEST/                        # Integration Tests (18+ modules)
│   ├── itest.smoke/
│   ├── itest.security/
│   └── ...
│
├── DEMO/                         # Demo Application
├── Docs/                         # Documentation
├── docs/                         # 🆕 New documentation (this file)
├── pom.xml                       # Root POM
└── README.md                     # Build instructions
```

---

## Module Groups Deep Dive

### 1. DEV Module - Core Rules Engine

**Location**: `/home/user/openl-tablets/DEV/`
**Purpose**: The heart of OpenL - compiles and executes business rules

#### Key Submodules:

**`org.openl.rules`** - The Main Engine (⭐ START HERE)
- **Size**: 1,200+ Java files
- **What it does**: Parses Excel, resolves types, generates bytecode, executes rules
- **Key packages**:
  - `org.openl.types` - Type system (`IOpenClass`, `IOpenMethod`, `IOpenField`)
  - `org.openl.binding` - Binds syntax to types
  - `org.openl.rules.dt` - Decision tables
  - `org.openl.rules.calc` - Spreadsheet tables
  - `org.openl.rules.runtime` - Execution engine
  - `org.openl.rules.lang.xls` - Excel parsing

**Entry Point**: `org.openl.rules.runtime.RulesEngineFactory`
```java
// Example: Compile and instantiate rules
RulesEngineFactory<MyRules> factory =
    new RulesEngineFactory<>("rules/MyRules.xlsx", MyRules.class);
MyRules rules = factory.newInstance();
```

**`org.openl.rules.project`** - Project Management
- Loads project descriptors (rules.xml)
- Manages multi-module projects
- Handles dependencies between modules
- **Entry Point**: `SimpleProjectEngineFactory`

**`org.openl.commons`** - Foundation
- Common utilities, formatters, file handling
- Domain abstractions
- Logging and version info

**`org.openl.spring`** - Spring Integration
- Property sources
- Conditional bean registration (`@ConditionalOnEnable`)

**Other Modules**:
- `org.openl.rules.annotations` - Custom annotations
- `org.openl.rules.util` - Built-in functions for rules
- `org.openl.rules.gen` - Code generation (build-time only)
- `org.openl.rules.constrainer` - Constraint solver
- `org.openl.rules.test` - Testing framework

**Dependency Flow**:
```
commons → rules → project → spring
    ↓
constrainer, annotations, util, gen, test
```

---

### 2. STUDIO Module - Web-Based IDE

**Location**: `/home/user/openl-tablets/STUDIO/`
**Purpose**: Web interface for creating, editing, and managing rules

#### Key Submodules:

**`org.openl.rules.webstudio`** - Main Web Application
- **Artifact**: `webapp.war`
- **Technology**: Spring Boot + JSF (legacy) + React (modern)
- **What it does**:
  - Rule authoring and editing
  - Project management
  - Repository integration (Git, AWS S3, Azure)
  - User management
  - Table editor

**Build Output**: `/STUDIO/org.openl.rules.webstudio/target/webapp.war`

**`studio-ui`** - Modern Frontend
- **Technology**: React 18, TypeScript, Ant Design
- **Location**: `/STUDIO/studio-ui/src/`
- **Structure**:
  ```
  studio-ui/src/
  ├── components/      # React components
  ├── pages/          # Page components
  ├── services/       # API services
  ├── stores/         # Zustand state management
  └── utils/          # Utilities
  ```
- **Build**: Uses Webpack, produces static assets

**`org.openl.rules.repository*`** - Repository Backends
- `org.openl.rules.repository` - Base abstraction
- `org.openl.rules.repository.git` - Git support (uses JGit)
- `org.openl.rules.repository.aws` - AWS S3 backend
- `org.openl.rules.repository.azure` - Azure Blob backend

**`org.openl.security*`** - Security Framework
- `org.openl.security` - Base framework
- `org.openl.security.standalone` - Built-in user management
- `org.openl.security.acl` - Access Control Lists

**Other Important Modules**:
- `org.openl.rules.tableeditor` - Table editing component
- `org.openl.rules.workspace` - Workspace management
- `org.openl.rules.jackson*` - JSON serialization
- `org.openl.rules.project.openapi*` - OpenAPI generation
- `org.openl.rules.diff` - Diff calculation
- `org.openl.rules.xls.merge` - Excel merge

**Entry URL**: http://localhost:8080 (when running locally)

---

### 3. WSFrontend Module - Rule Services

**Location**: `/home/user/openl-tablets/WSFrontend/`
**Purpose**: Deploys rules as REST web services

#### Key Submodules:

**`org.openl.rules.ruleservice`** - Core Service Engine
- Loads rules from repositories
- Manages rule lifecycle
- Handles versioning and deployment
- **Key classes**:
  - `RuleServiceLoader` - Loads rules
  - `RuleServiceDeployer` - Deploys services
  - `RuleServiceManager` - Manages lifecycle

**`org.openl.rules.ruleservice.ws`** - Web Services
- **Artifact**: `webapp.war`
- **Protocols**: REST (CXF)
- **Features**:
  - Auto-generates service endpoints from rules
  - Swagger/OpenAPI documentation
  - Request/response logging
  - Authentication integration

**Build Output**: `/WSFrontend/org.openl.rules.ruleservice.ws/target/webapp.war`

**`org.openl.rules.ruleservice.kafka`** - Kafka Integration
- Message-driven rule execution
- Async rule invocation
- Event-driven architecture support

**`org.openl.rules.ruleservice.deployer`** - Deployment Manager
- Hot deployment
- Zero-downtime updates
- Deployment filters

**Logging Modules**:
- `org.openl.rules.ruleservice.ws.storelogdata*` - Request/response logging
- Database-backed log storage

**Entry URL**: http://localhost:8081 (when running locally)

---

### 4. Util Module - Tools & Utilities

**Location**: `/home/user/openl-tablets/Util/`
**Purpose**: Developer tools, Maven plugins, archetypes

#### Key Submodules:

**`openl-maven-plugin`** - Maven Plugin
- Compiles OpenL rules during Maven build
- Generates Java classes from rules
- Goal: `openl:compile`

**Usage**:
```xml
<plugin>
    <groupId>org.openl</groupId>
    <artifactId>openl-maven-plugin</artifactId>
    <version>6.0.0-SNAPSHOT</version>
</plugin>
```

**`openl-*-archetype`** - Maven Archetypes
- Project templates
- Quick start scaffolding

**`openl-openapi-*`** - OpenAPI Tools
- Parses OpenAPI specs
- Generates OpenL types from OpenAPI models
- Scaffolds Excel tables from API definitions

**`org.openl.rules.profiler`** - Performance Profiler
- Rules execution profiling
- Performance metrics

**`openl-rules-opentelemetry`** - OpenTelemetry
- Distributed tracing
- Observability integration

---

### 5. ITEST Module - Integration Tests

**Location**: `/home/user/openl-tablets/ITEST/`
**Purpose**: End-to-end testing of OpenL components

**Test Coverage**:
- Smoke tests
- Security tests (SAML, CAS, JWT)
- WebStudio tests
- Kafka integration
- Cloud storage (MinIO)
- Spring Boot integration
- Health checks
- Performance tests

**Test Infrastructure**:
- **TestContainers**: Docker-based tests
- **server-core**: Shared test server

---

## Common Navigation Patterns

### Finding Core Engine Code

**Question**: Where is rule compilation logic?

**Answer**:
1. Start: `/DEV/org.openl.rules/src/org/openl/engine/OpenLCompileManager.java`
2. Parser: `/DEV/org.openl.rules/src/org/openl/rules/lang/xls/Parser.java`
3. Binding: `/DEV/org.openl.rules/src/org/openl/binding/`
4. Bytecode gen: `/DEV/org.openl.rules/src/org/openl/rules/runtime/` (uses ASM)

### Finding UI Code

**Question**: Where is the WebStudio UI?

**Answer**:
1. **Modern React UI**: `/STUDIO/studio-ui/src/`
2. **Legacy JSF**: `/STUDIO/org.openl.rules.webstudio/src/main/webapp/`
3. **Table Editor**: `/STUDIO/org.openl.rules.tableeditor/src/`

### Finding REST API Code

**Question**: Where are REST endpoints defined?

**Answer**:
1. **RuleService**: `/WSFrontend/org.openl.rules.ruleservice.ws/src/` (CXF-based)
2. **WebStudio**: `/STUDIO/org.openl.rules.webstudio/src/` (Spring REST controllers)

### Finding Configuration

**Question**: Where is system configuration?

**Answer**:
1. **Project config**: `rules.xml` (project descriptors)
2. **Application properties**: `application.properties` in WAR modules
3. **Spring config**: `@Configuration` classes in each module
4. **Maven config**: `pom.xml` files

---

## Key Concepts

### 1. Project Structure

OpenL projects have this structure:
```
my-rules-project/
├── rules.xml                    # Project descriptor
├── rules/                       # Rules files
│   ├── MyDecisionTable.xlsx
│   ├── MyDataTable.xlsx
│   └── MySpreadsheet.xlsx
└── pom.xml                      # Maven config (optional)
```

**rules.xml** example:
```xml
<project>
    <name>My Rules</name>
    <modules>
        <module>
            <name>main</name>
            <rules-root path="rules"/>
        </module>
    </modules>
</project>
```

### 2. Table Types

OpenL supports multiple table types in Excel:

| Table Type | Purpose | Example |
|------------|---------|---------|
| **Decision Table** | Condition-action rules | Insurance premium calculation |
| **Data Table** | Structured data | Rate tables, lookup tables |
| **Spreadsheet** | Excel-like calculations | Financial calculations |
| **Method Table** | Custom methods | Business logic methods |
| **Test Table** | Unit tests | Test cases for rules |
| **Datatype Table** | Custom types | Domain objects |

### 3. Type System

OpenL has its own type system parallel to Java:

- **`IOpenClass`** - Equivalent to Java `Class`
- **`IOpenMethod`** - Equivalent to Java `Method`
- **`IOpenField`** - Equivalent to Java `Field`

**Why?** Allows dynamic types from Excel tables.

### 4. Compilation Flow

```
1. Excel File
   ↓
2. Parser (JavaCC BExGrammar) → Syntax Tree (ISyntaxNode)
   ↓
3. Binder → Bound Tree (IBoundNode)
   ↓
4. Type Resolution → IOpenClass hierarchy
   ↓
5. Code Generation (ASM) → Java bytecode
   ↓
6. Proxy Generation → Service interface
   ↓
7. Execution → Method invocation
```

### 5. Execution Model

**At runtime**:
1. Generated proxy intercepts method calls
2. Proxy looks up compiled rule method
3. Method executes in `SimpleRulesVM` environment
4. Result returned to caller

**Context Management**:
- `IRulesRuntimeContext` - Holds execution context (date, region, etc.)
- Used for rule versioning and variant selection

---

## Build Artifacts

### Main Artifacts

| Artifact | Location | Type | Purpose |
|----------|----------|------|---------|
| OpenL Studio | `STUDIO/org.openl.rules.webstudio/target/webapp.war` | WAR | Web IDE |
| RuleService WS | `WSFrontend/org.openl.rules.ruleservice.ws/target/webapp.war` | WAR | Rule services |
| Demo App | `DEMO/target/openl-tablets-demo.zip` | ZIP | Demo application |

### Library Artifacts

All modules publish to Maven Central:
- Group ID: `org.openl`
- Artifact IDs: `org.openl.core`, `org.openl.rules`, etc.

---

## Development Workflows

### Running Locally

**Option 1: Docker Compose** (Easiest)
```bash
docker compose up
# Open http://localhost
```

**Option 2: Build and Run**
```bash
# Build
mvn clean install -DskipTests

# Run Studio
cd STUDIO/org.openl.rules.webstudio
mvn jetty:run

# Open http://localhost:8080
```

### Running Tests

```bash
# All tests
mvn test

# Quick tests only
mvn test -Dquick

# Specific module
cd DEV/org.openl.rules
mvn test

# Integration tests
cd ITEST/itest.smoke
mvn verify
```

### Making Changes

1. **Modify code** in relevant module
2. **Run tests** to verify
3. **Build** the module
4. **Test integration** if needed

---

## Common Tasks

### Task: Add a New Table Type

**Modules**: `org.openl.rules`
**Steps**:
1. Define table syntax in Excel
2. Create node binder in `org.openl.rules.lang.xls.binding`
3. Create bound node in `org.openl.rules.binding`
4. Register in `XlsDefinitions`
5. Add tests

### Task: Add REST Endpoint to Studio

**Modules**: `org.openl.rules.webstudio`
**Steps**:
1. Create `@RestController` in `org.openl.rules.webstudio.web.rest`
2. Implement endpoint logic
3. Add tests

### Task: Add UI Feature

**Modules**: `studio-ui`
**Steps**:
1. Create React component in `studio-ui/src/components`
2. Add routing if needed
3. Connect to backend API
4. Add i18n translations
5. Test

### Task: Extend Security

**Modules**: `org.openl.security*`
**Steps**:
1. Extend `org.openl.security` base classes
2. Implement authentication provider
3. Configure in Spring Security
4. Test with integration tests

---

## Important Files to Know

| File | Location | Purpose |
|------|----------|---------|
| `pom.xml` | `/` | Root Maven configuration |
| `README.md` | `/` | Build instructions |
| `docker-compose.yaml` | `/` | Docker setup |
| `bexgrammar.jj` | `/DEV/org.openl.rules/grammar/` | Parser grammar (JavaCC) |
| `rules.xml` | (project-specific) | Project descriptor |
| `application.properties` | (each WAR module) | Spring configuration |

---

## Code Quality & Standards

### Design Patterns
- **Factory**: `RulesEngineFactory`, node binder factories
- **Builder**: `SimpleProjectEngineFactoryBuilder`
- **Strategy**: Instantiation strategies, resolving strategies
- **Proxy**: ASM runtime proxies
- **Visitor**: Syntax node traversal

### Coding Conventions
- **Java**: Standard Java conventions
- **TypeScript**: Standard TS conventions (ESLint)
- **Testing**: JUnit 5, Mockito for mocking
- **Logging**: SLF4J facade

### Architecture Principles
- **Separation of Concerns**: Clear module boundaries
- **Dependency Injection**: Spring-based DI where applicable
- **Extensibility**: Plugin-based architecture for new features
- **Convention over Configuration**: Sensible defaults

---

## Getting Help

### Documentation
- **Project Docs**: `/home/user/openl-tablets/Docs/`
- **Architecture Docs**: `/home/user/openl-tablets/docs/architecture/`
- **Module Analysis**: `/home/user/openl-tablets/docs/analysis/`

### Key Resources
- **Website**: https://openl-tablets.org
- **GitHub**: https://github.com/openl-tablets/openl-tablets
- **Maven Central**: Search for `org.openl`

### Where to Start Reading Code
1. **Core Engine**: `/DEV/org.openl.rules/src/org/openl/rules/runtime/RulesEngineFactory.java`
2. **Project Loading**: `/DEV/org.openl.rules.project/src/org/openl/rules/project/instantiation/SimpleProjectEngineFactory.java`
3. **Decision Tables**: `/DEV/org.openl.rules/src/org/openl/rules/dt/IDecisionTable.java`
4. **Type System**: `/DEV/org.openl.rules/src/org/openl/types/IOpenClass.java`

---

## Next Steps

1. **Read**: [Technology Stack](/docs/architecture/technology-stack.md) - Understand the tech
2. **Setup**: [Development Setup](/docs/onboarding/development-setup.md) - Get environment ready
3. **Deep Dive**: [DEV Module Overview](/docs/analysis/dev-module-overview.md) - Understand core engine
4. **Build**: Follow `/README.md` to build the project
5. **Explore**: Clone and navigate the code

---

**Welcome to OpenL Tablets! Happy coding!**

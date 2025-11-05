# OpenL Tablets Architecture

**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-06

---

## Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Module Breakdown](#module-breakdown)
- [Technology Stack](#technology-stack)
- [Core Components](#core-components)
- [Data Flow](#data-flow)
- [Design Patterns](#design-patterns)
- [Extension Points](#extension-points)
- [Performance Characteristics](#performance-characteristics)
- [Security Architecture](#security-architecture)
- [Deployment Models](#deployment-models)

---

## Overview

OpenL Tablets is an enterprise-grade Business Rules Management System (BRMS) that compiles Excel-based business rules into executable Java bytecode. The system bridges the gap between business users (who write rules in Excel) and developers (who integrate these rules into applications).

### Key Architectural Goals

- **Separation of Concerns**: Business logic (Excel rules) separated from technical implementation
- **Type Safety**: Strong compile-time type checking prevents runtime errors
- **Performance**: Excel rules compiled to native JVM bytecode for maximum speed
- **Extensibility**: Plugin architecture for custom table types, data sources, and integrations
- **Maintainability**: Clear layered architecture with well-defined module boundaries
- **Scalability**: Stateless engine design supports horizontal scaling

---

## System Architecture

### High-Level Architecture

```
┌───────────────────────────────────────────────────────────────────────────┐
│                              User Interfaces                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐         │
│  │  OpenL Studio    │  │  Rule Services   │  │  MCP Server      │         │
│  │  (Web IDE)       │  │  (REST)          │  │  (AI Tools)      │         │
│  │  React + JSF     │  │  Spring Boot     │  │  TypeScript      │         │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘         │
└───────────┼─────────────────────┼─────────────────────┼───────────────────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  │
┌─────────────────────────────────┼─────────────────────────────────────────┐
│                         Application Layer                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │  Generated Java Interfaces (Proxy Pattern)                          │  │
│  │  - Type-safe method signatures                                      │  │
│  │  - Automatic Excel → Java mapping                                   │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────┼────────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼──────────────────────────────────────┐
│                         Rules Engine (Core)                              │
│                                                                          │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌────────────┐ │
│  │   Parser     │──→│   Binder     │──→│ Type System  │──→│ Bytecode   │ │
│  │              │   │              │   │              │   │ Generator  │ │
│  │  JavaCC      │   │  Resolution  │   │  IOpenClass  │   │    ASM     │ │
│  │  BExGrammar  │   │  Type Check  │   │  IOpenMethod │   │            │ │
│  └──────────────┘   └──────────────┘   └──────────────┘   └─────┬──────┘ │
│                                                                 │        │
│                           ┌─────────────────────────────────────┘        │
│                           │                                              │
│  ┌────────────────────────▼─────────────────────────────┐                │
│  │                Runtime Environment (VM)              │                │
│  │  - Method dispatch                                   │                │
│  │  - Runtime context management                        │                │
│  │  - Compiled bytecode execution                       │                │
│  └──────────────────────────────────────────────────────┘                │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼──────────────────────────────────────┐
│                         Data Layer                                       │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌────────────┐ │
│  │  Excel Files │   │  Git Repos   │   │  Database    │   │  File      │ │
│  │  (.xls/.xlsx)│   │  (Projects)  │   │  (Metadata)  │   │  System    │ │
│  └──────────────┘   └──────────────┘   └──────────────┘   └────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘
```

### Layered Architecture

OpenL Tablets follows a strict layered architecture:

| Layer | Purpose | Key Components |
|-------|---------|----------------|
| **Presentation** | User interfaces and API endpoints | OpenL Studio, Rule Services, MCP Server |
| **Application** | Generated proxies and facades | Interface generation, method routing |
| **Business Logic** | Rules engine and compiler | Parser, Binder, Type System, Bytecode Generator |
| **Runtime** | Execution environment | VM, Context Management, Method Dispatch |
| **Data** | Persistent storage | Excel files, Git, Database, File System |

**Architectural Principle**: Lower layers never depend on higher layers. Each layer communicates only with adjacent layers.

---

## Module Breakdown

### DEV Module Group (Core Engine)

**Location**: `/DEV/`
**Purpose**: Core rules engine, compiler, and runtime

#### Key Modules

| Module | Lines of Code | Purpose |
|--------|--------------|---------|
| **org.openl.core** | ~15K | Core abstractions (IOpenClass, IOpenMethod, IOpenField) |
| **org.openl.rules** | ~150K | Rules engine, parser, binder, type system |
| **org.openl.rules.calc** | ~8K | Spreadsheet cell calculations |
| **org.openl.rules.dt** | ~25K | Decision table implementation |
| **org.openl.rules.data** | ~12K | Datatype tables and data binding |
| **org.openl.rules.runtime** | ~5K | Runtime environment and bytecode execution |
| **org.openl.rules.project** | ~18K | Project model and dependency resolution |
| **org.openl.rules.webstudio.lib** | ~3K | WebStudio integration libraries |

#### Architecture Layers within DEV

```
Application Layer (Generated Proxies)
         ↓
Runtime Layer (VM, Context)
         ↓
Binding Layer (Type Resolution)
         ↓
Parsing Layer (BExGrammar)
         ↓
Source Layer (Excel, Files)
```

### STUDIO Module Group (Web IDE)

**Location**: `/STUDIO/`
**Purpose**: Web-based IDE for rule development

#### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React)                          │
│  - Modern UI components                                      │
│  - State management (Zustand)                                │
│  - REST API client                                           │
│  Location: /STUDIO/studio-ui/                                │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP/WebSocket
┌──────────────────────┴──────────────────────────────────────┐
│              Backend (Spring Boot)                           │
│  - REST Controllers                                          │
│  - Project management                                        │
│  - Git integration                                           │
│  - Rule compilation                                          │
│  Location: /STUDIO/org.openl.rules.webstudio/               │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────────┐
│                 Legacy JSF UI                                │
│  - RichFaces components (forked)                             │
│  - Being migrated to React                                   │
│  Status: Maintenance mode only                               │
└─────────────────────────────────────────────────────────────┘
```

**Technology Stack:**
- **Frontend**: React 18.3.1, TypeScript 5.8.3, Zustand, Axios
- **Backend**: Spring Boot 3.5.6, Spring Security, Spring Data JPA
- **Legacy**: JSF 2.3, RichFaces (forked), Facelets

### WSFrontend Module Group (Rule Services)

**Location**: `/WSFrontend/`
**Purpose**: Deploy rules as REST web services

#### Architecture

```
┌──────────────────────────────────────────┐
│                    API Layer             │
│  ┌────────────────┐    ┌──────────────┐  │
│  │  REST          │    │  OpenAPI     │  │
│  │  Spring MVC    │    │  Swagger     │  │
│  └────────┬───────┘    └───────┬──────┘  │
└───────────┼────────────────────┼─────────┘
            │                    │                    
┌───────────┴────────────────────┴─────────┐
│              Service Deployment Layer    │
│  - Dynamic service instantiation         │
│  - Hot reload of rule changes            │
│  - Version management                    │
│  - Multi-project support                 │
└───────────────────────────────┬──────────┘
                                │
┌───────────────────────────────┴──────────┐
│                      Rules Engine        │
│  (Delegates to DEV modules)              │
└──────────────────────────────────────────┘
```

**Key Features:**
- **Hot Reload**: Detects Excel file changes and recompiles automatically
- **Multi-Version**: Serve multiple versions of the same ruleset simultaneously
- **OpenAPI**: Auto-generated OpenAPI 3.0 documentation
- **Caching**: Intelligent caching of compiled rules

### Util Module Group

**Location**: `/Util/`
**Purpose**: Developer tools and utilities

- **org.openl.rules.maven.plugin**: Maven plugin for rule compilation
- **org.openl.rules.eclipse**: Eclipse IDE integration (deprecated)
- **org.openl.conf.ant**: Ant tasks for rule deployment

### ITEST Module Group

**Location**: `/ITEST/`
**Purpose**: Integration and end-to-end testing

- Uses TestContainers for Docker-based tests
- Tests full stack: Excel → Compilation → Runtime → Services
- Multi-database compatibility tests

---

## Technology Stack

### Backend Technologies

| Technology | Version | Purpose                                |
|-----------|---------|----------------------------------------|
| **Java** | 21+ | Primary language, bytecode target      |
| **Spring Boot** | 3.5.6 | Application framework                  |
| **Spring Framework** | 6.2.11 | Dependency injection, AOP, data access |
| **Apache CXF** | 4.1.0 | REST web services                      |
| **ASM** | 9.7.1 | Bytecode manipulation                  |
| **JavaCC** | 7.0.13 | Parser generator (BExGrammar)          |
| **Apache POI** | 5.3.0 | Excel file parsing                     |
| **Hibernate** | 6.6.4 | ORM for metadata storage               |
| **Liquibase** | 4.31.0 | Database migrations                    |

### Frontend Technologies

| Technology | Version | Purpose |
|-----------|---------|---------|
| **React** | 18.3.1 | Modern UI framework |
| **TypeScript** | 5.8.3 | Type-safe JavaScript |
| **Zustand** | 5.0.2 | State management |
| **Axios** | 1.7.9 | HTTP client |
| **Vite** | 6.0.7 | Build tool |
| **JSF** | 2.3 | Legacy UI (maintenance mode) |

### Build and DevOps

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Maven** | 3.9.9+ | Build automation |
| **Docker** | 27.5.0+ | Containerization |
| **TestNG** | 7.10.2 | Testing framework |
| **JUnit 5** | 5.11.4 | Testing framework (modern tests) |
| **Mockito** | 5.14.2 | Mocking framework |
| **Spotless** | 2.43.0 | Code formatting |
| **Checkstyle** | 10.21.1 | Code quality |
| **JaCoCo** | 0.8.12 | Code coverage |

### Supported Databases

| Database | Versions | JDBC Driver |
|----------|----------|-------------|
| **PostgreSQL** | 12, 13, 14, 15, 16 | 42.7.4 |
| **MySQL** | 5.7, 8.0, 8.4 | 9.2.0 |
| **MariaDB** | 10.5, 10.6, 11.2 | 3.5.1 |
| **Oracle** | 12c, 19c, 21c | 23.6.0.24.10 |
| **H2** | 2.3.232 | 2.3.232 (embedded) |

---

## Core Components

### 1. Parser (BExGrammar)

**Location**: `/DEV/org.openl.rules/grammar/bexgrammar.jj`
**Technology**: JavaCC (Java Compiler Compiler)

**Purpose**: Parses OpenL expressions and Excel cell formulas into Abstract Syntax Trees (AST).

**Grammar Features:**
- Expression parsing: `price * 1.1 + tax`
- Method calls: `calculatePremium(age, coverage)`
- Type references: `Driver`, `Policy[]`
- Special operators: `select all`, `order by`, `where`

**Output**: `ISyntaxNode` tree representing parsed expressions

### 2. Binder (Type Resolution)

**Location**: `/DEV/org.openl.rules/src/org/openl/binding/`

**Purpose**: Resolves types, methods, and fields; performs type checking.

**Key Classes:**
- `MethodSearch`: Finds matching methods with overload resolution
- `TypeCast`: Handles implicit and explicit type conversions
- `BindHelper`: Central binding coordination
- Node binders: `MethodNodeBinder`, `BinaryNodeBinder`, etc.

**Process:**
1. Resolve identifiers to types/methods/fields
2. Type-check expressions
3. Insert implicit type casts
4. Validate method signatures
5. Build bound syntax tree (`IBoundNode`)

**Example:**
```java
// Excel: =calculatePremium(age, "Standard")
// Binder resolves:
//   - calculatePremium → IOpenMethod
//   - age → IOpenField (from context)
//   - "Standard" → String constant
//   - Validates types match method signature
```

### 3. Type System

**Location**: `/DEV/org.openl.rules/src/org/openl/types/`

**Purpose**: OpenL's parallel type system (distinct from Java reflection).

**Core Interfaces:**

```java
public interface IOpenClass {
    String getName();
    IOpenMethod getMethod(String name, IOpenClass[] params);
    IOpenField getField(String name);
    Object newInstance(IRuntimeEnv env);
    // ... more methods
}

public interface IOpenMethod {
    Object invoke(Object target, Object[] params, IRuntimeEnv env);
    IOpenClass getType();  // Return type
    IOpenClass getDeclaringClass();
    // ... more methods
}

public interface IOpenField {
    Object get(Object target, IRuntimeEnv env);
    void set(Object target, Object value, IRuntimeEnv env);
    IOpenClass getType();
    // ... more methods
}
```

**Type Hierarchy:**
- `JavaOpenClass`: Wrapper for Java classes
- `DatatypeOpenClass`: Custom Excel-defined types
- `DomainOpenClass`: Restricted value domains
- `ArrayOpenClass`: Array types
- `ModuleOpenClass`: Represents an Excel module

**Why a separate type system?**
- Enables dynamic types from Excel
- Supports compile-time type checking of rules
- Allows custom type behaviors (e.g., domain constraints)
- Independent of Java's reflection API

### 4. Bytecode Generator

**Location**: `/DEV/org.openl.rules/src/org/openl/rules/runtime/`
**Technology**: ASM (ObjectWeb ASM library)

**Purpose**: Generates JVM bytecode for compiled rules.

**Process:**
1. Traverse bound syntax tree
2. Emit bytecode instructions (using ASM)
3. Generate method implementations
4. Create proxy classes

**Example:**
```java
// Excel rule:
// premium = basePremium * 1.1

// Generated bytecode (pseudo-assembly):
ALOAD 1         // Load basePremium
LDC 1.1         // Load constant 1.1
DMUL            // Multiply
DSTORE 2        // Store in premium
RETURN
```

**Optimizations:**
- Inline constants
- Optimize method dispatch
- Eliminate unnecessary casts

**Why bytecode generation?**
- Native JVM performance (no interpretation overhead)
- JIT compiler optimizations apply
- Type safety enforced at bytecode level

### 5. Runtime Environment

**Location**: `/DEV/org.openl.rules/src/org/openl/vm/`

**Key Classes:**
- `IRuntimeEnv`: Runtime context (variables, stack)
- `IRunner`: Executes compiled rules
- `SimpleVM`: Lightweight VM for expression evaluation

**Runtime Context:**
```java
public interface IRuntimeEnv {
    Object getLocalFrame();
    Object[] getLocalFrames();
    Object getThis();
    IRuntimeContext getContext();
    // ... more methods
}
```

**Thread Safety**: Each thread gets its own `IRuntimeEnv` instance.

### 6. Table Types

OpenL Tablets supports multiple table types for different rule patterns:

| Table Type | Purpose | Example Use Case |
|-----------|---------|------------------|
| **Decision Table** | Multi-dimensional rule lookups | Insurance premium calculation |
| **Spreadsheet** | Cell-based calculations | Mortgage amortization |
| **Datatype** | Custom type definitions | Policy, Driver, Vehicle types |
| **Data** | Test data and lookup tables | State codes, tax rates |
| **Method** | Reusable functions | calculateDiscount() |
| **Test** | Unit tests for rules | Test premium calculation |
| **Run** | Test execution | Execute multiple tests |
| **Properties** | Metadata and versioning | Effective dates, regions |

---

## Data Flow

### Compilation Flow

```
┌─────────────┐
│ Excel File  │
│ (.xls/.xlsx)│
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│  Apache POI Parser  │
│  - Read workbook    │
│  - Extract tables   │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Table Parser       │
│  - Identify type    │
│  - Parse structure  │
│  - BExGrammar       │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Syntax Tree (AST)  │
│  ISyntaxNode        │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Binder             │
│  - Type resolution  │
│  - Method binding   │
│  - Type checking    │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Bound Tree         │
│  IBoundNode         │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Bytecode Generator │
│  - ASM library      │
│  - Emit bytecode    │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Compiled Class     │
│  (JVM bytecode)     │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  ClassLoader        │
│  - Load into JVM    │
│  - Create instance  │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  Runtime Instance   │
│  (Ready to execute) │
└─────────────────────┘
```

### Runtime Execution Flow

```
┌────────────────┐
│ Client Request │ (e.g., REST API call)
└────────┬───────┘
         │
         ▼
┌────────────────────────┐
│ Generated Proxy        │
│ (Type-safe interface)  │
└────────┬───────────────┘
         │
         ▼
┌────────────────────────┐
│ Method Dispatch        │
│ - Resolve method       │
│ - Prepare context      │
└────────┬───────────────┘
         │
         ▼
┌────────────────────────┐
│ Compiled Bytecode      │
│ (Executes natively)    │
└────────┬───────────────┘
         │
         ▼
┌────────────────────────┐
│ Rule Execution         │
│ - Decision tables      │
│ - Spreadsheets         │
│ - Method calls         │
└────────┬───────────────┘
         │
         ▼
┌────────────────────────┐
│ Result                 │
│ (Return to client)     │
└────────────────────────┘
```

---

## Design Patterns

### 1. Factory Pattern

**Usage**: Creating rules engine instances

```java
// Simple factory
RulesEngineFactory<MyRules> factory =
    new RulesEngineFactory<>("rules.xlsx", MyRules.class);
MyRules rules = factory.newInstance();

// Complex factory (Builder pattern)
SimpleProjectEngineFactory<MyRules> factory =
    new SimpleProjectEngineFactoryBuilder<MyRules>()
        .setProject("path/to/project")
        .setInterfaceClass(MyRules.class)
        .setExecutionMode(true)
        .setProvideRuntimeContext(true)
        .build();
```

### 2. Visitor Pattern

**Usage**: Traversing syntax trees

```java
public interface ISyntaxNode {
    void accept(ISyntaxNodeVisitor visitor);
}

public class TypeResolverVisitor implements ISyntaxNodeVisitor {
    public void visit(MethodCall node) {
        // Resolve method signature
    }

    public void visit(BinaryOp node) {
        // Resolve operator overload
    }
}
```

### 3. Strategy Pattern

**Usage**: Pluggable algorithms

```java
public interface RulesInstantiationStrategy {
    Object instantiate(CompiledOpenClass compiledClass);
}

// Different strategies:
// - ApiBasedInstantiationStrategy: Generate proxy
// - CommonRulesInstantiationStrategy: Direct instantiation
```

### 4. Proxy Pattern

**Usage**: Generated interfaces for rules

```java
// User defines interface:
public interface InsuranceRules {
    double calculatePremium(Driver driver, Vehicle vehicle);
}

// OpenL generates proxy that:
// 1. Validates input types
// 2. Prepares runtime context
// 3. Invokes compiled bytecode
// 4. Returns result
```

### 5. Template Method Pattern

**Usage**: Table binding

```java
public abstract class ATableBinder {
    public final IBoundNode bindTable(ISyntaxNode table) {
        validate(table);           // Hook
        parseHeader(table);        // Hook
        parseBody(table);          // Hook
        return createBoundNode();  // Hook
    }

    protected abstract void validate(ISyntaxNode table);
    protected abstract void parseHeader(ISyntaxNode table);
    // ...
}
```

### 6. Singleton Pattern

**Usage**: Type caches, configuration

```java
// JavaOpenClassCache (thread-safe singleton)
public class JavaOpenClassCache {
    private static final JavaOpenClassCache INSTANCE =
        new JavaOpenClassCache();

    public static JavaOpenClassCache getInstance() {
        return INSTANCE;
    }
}
```

---

## Extension Points

OpenL Tablets provides multiple extension points for customization:

### 1. Custom Table Types

**Extension Point**: `/DEV/org.openl.rules/src/org/openl/rules/lang/xls/binding/`

**Steps to Add**:
1. Create table syntax definition (Excel format)
2. Implement `ATableBinder` for parsing
3. Implement `IBoundNode` for bound representation
4. Register in `XlsDefinitions`
5. Optionally implement custom bytecode generation

**Example Use Case**: Add "Decision Tree" table type

### 2. Custom Data Sources

**Extension Point**: `/DEV/org.openl.rules/src/org/openl/rules/project/`

**Interface**: `IDependencyManager`, `IProjectSource`

**Example Use Case**: Load rules from REST API instead of file system

### 3. Custom Type Providers

**Extension Point**: `/DEV/org.openl.core/src/org/openl/types/`

**Interface**: `IOpenClass` implementations

**Example Use Case**: Map rules to database entities

### 4. Custom Node Binders

**Extension Point**: `/DEV/org.openl.rules/src/org/openl/binding/impl/`

**Base Class**: `ANodeBinder`

**Example Use Case**: Add support for new expression syntax

### 5. MCP Servers and Tools

**Extension Point**: `/STUDIO/org.openl.rules.mcp/`

**Protocol**: Model Context Protocol (MCP)

**Example Use Case**: Custom AI assistant integration

---

## Performance Characteristics

### Compilation Performance

| Operation | Typical Time | Notes |
|-----------|-------------|-------|
| **Parse Excel file** | 50-500 ms | Depends on file size |
| **Bind types** | 100-1000 ms | Depends on rule complexity |
| **Generate bytecode** | 50-200 ms | Per class |
| **Load class** | 10-50 ms | Per class |
| **Full compilation** | 1-10 seconds | For typical project |

**Optimization Tips**:
- Cache compiled classes (avoid recompilation)
- Use lazy loading for large projects
- Parallelize compilation of independent modules

### Runtime Performance

| Operation | Typical Time | Notes |
|-----------|-------------|-------|
| **Method dispatch** | ~1 ns | Native bytecode |
| **Decision table lookup** | 10-1000 ns | Depends on table size and indexing |
| **Spreadsheet calculation** | 100-10000 ns | Depends on cell count |
| **Type conversion** | 1-10 ns | Cached conversions |

**Why So Fast?**
- **Bytecode compilation**: No interpretation overhead
- **JIT optimization**: HotSpot JIT applies
- **Indexing**: Decision tables use hash-based indexes
- **Caching**: Type conversions, method lookups cached

**Benchmark Example** (Decision table with 1000 rows):
- Cold lookup: ~500 ns
- Hot lookup (JIT optimized): ~50 ns

### Memory Characteristics

| Component | Memory Usage | Notes |
|-----------|--------------|-------|
| **Compiled class** | 5-50 KB | Per rule class |
| **Type metadata** | 1-10 MB | For typical project |
| **Runtime context** | 1-5 KB | Per thread |
| **Excel file (loaded)** | 2-20 MB | Depends on file size |

**Memory Optimization**:
- Unload Excel after compilation (not needed at runtime)
- Share type metadata across instances
- Use thread-local contexts (no synchronization)

---

## Security Architecture

### Authentication

**Supported Methods**:
- **Form-based**: Username/password with database storage
- **LDAP/Active Directory**: Enterprise directory integration
- **SAML 2.0**: Single Sign-On (SSO)
- **OAuth 2.0**: Third-party authentication
- **CAS**: Central Authentication Service

**Configuration**: `/STUDIO/org.openl.rules.webstudio/src/main/resources/security/`

### Authorization

**Model**: Role-Based Access Control (RBAC)

**Roles**:
- **Admin**: Full system access
- **Deployer**: Deploy rule services
- **Developer**: Edit rules, run tests
- **Analyst**: View and test rules
- **Viewer**: Read-only access

**Permissions**:
- Project-level (per OpenL project)
- Operation-level (create, read, update, delete, deploy)
- Resource-level (specific files or tables)

### Security Best Practices

1. **Input Validation**:
   - Validate all Excel inputs during compilation
   - Type-check method parameters at runtime
   - Sanitize file paths and URLs

2. **Sandbox Execution**:
   - Limit access to system classes (e.g., `System.exit()`)
   - Control reflection usage
   - Restrict file system access

3. **Sensitive Data**:
   - Never log passwords or tokens
   - Use `PassCoder` for password encryption
   - Encrypt sensitive configuration properties

4. **API Security**:
   - Use HTTPS in production
   - Implement rate limiting
   - Enable CORS only for trusted origins
   - Validate OpenAPI parameters

---

## Deployment Models

### 1. Embedded Mode

**Use Case**: Rules embedded in custom application

```java
// Your application
public class MyApp {
    private final InsuranceRules rules;

    public MyApp() {
        RulesEngineFactory<InsuranceRules> factory =
            new RulesEngineFactory<>("rules.xlsx", InsuranceRules.class);
        this.rules = factory.newInstance();
    }

    public void processPolicy(Policy policy) {
        double premium = rules.calculatePremium(policy);
        // ... use result
    }
}
```

**Characteristics**:
- Rules packaged with application
- No external dependencies
- Full control over lifecycle
- Suitable for batch processing, microservices

### 2. Standalone Mode (Rule Services)

**Use Case**: Rules deployed as REST services

```
┌─────────────────┐      HTTP/REST      ┌─────────────────┐
│ Client App      │ ──────────────────→ │ Rule Service    │
│ (Any language)  │                     │ (WSFrontend)    │
└─────────────────┘                     └─────────────────┘
```

**Characteristics**:
- Rules hosted in separate service
- Multiple clients can consume
- Hot reload of rule changes
- OpenAPI documentation
- Horizontal scaling

**Deployment**: Docker, Kubernetes, Tomcat, WildFly

### 3. Hybrid Mode

**Use Case**: OpenL Studio + Rule Services

```
┌─────────────────┐      Git Sync       ┌─────────────────┐
│ OpenL Studio    │ ──────────────────→ │ Rule Service    │
│ (Development)   │                      │ (Production)    │
└─────────────────┘                      └─────────────────┘
```

**Workflow**:
1. Develop rules in OpenL Studio
2. Commit to Git repository
3. Rule Service pulls changes
4. Hot reload (no downtime)

**Characteristics**:
- Clear separation: dev vs. prod
- Version control integration
- Audit trail
- Rollback capability

### 4. Cloud Native

**Platforms**: AWS, Azure, GCP, OpenShift

**Architecture**:
```
Load Balancer
      │
      ├─→ Rule Service Pod 1
      ├─→ Rule Service Pod 2
      └─→ Rule Service Pod N
            │
            └─→ Shared Database (PostgreSQL RDS)
```

**Features**:
- Auto-scaling based on load
- Health checks and self-healing
- Rolling updates
- Multi-region deployment
- Managed databases

**Kubernetes Example**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: openl-rule-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: openl
  template:
    metadata:
      labels:
        app: openl
    spec:
      containers:
      - name: rule-service
        image: openltablets/ws:6.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://db:5432/openl
```

---

## Summary

OpenL Tablets' architecture is designed for:

- **Performance**: Bytecode compilation for native JVM speed
- **Type Safety**: Compile-time validation prevents runtime errors
- **Extensibility**: Plugin architecture for custom table types and integrations
- **Scalability**: Stateless design supports horizontal scaling
- **Maintainability**: Clear layered architecture with well-defined boundaries
- **Enterprise Ready**: Battle-tested in production environments

For more details on specific components, see:
- [DEV Module Architecture](architecture/dev-module.md)
- [STUDIO Architecture](architecture/studio-module.md)
- [WSFrontend Architecture](architecture/wsfrontend-module.md)
- [Performance Tuning Guide](guides/performance-tuning.md)

---

**Questions or suggestions?** Open an issue on [GitHub](https://github.com/openl-tablets/openl-tablets/issues).

# DEV Module - Core Rules Engine Overview

**Module Location**: `/home/user/openl-tablets/DEV/`
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-05

---

## Executive Summary

The DEV module is the **heart of OpenL Tablets**, containing the complete business rules engine that compiles Excel spreadsheets into executable Java bytecode. It consists of 9 submodules organized in a layered architecture:

```
Foundation (commons) → Core Engine (rules) → Project Management → Integration (spring)
```

**Total Size**: ~1,900+ Java files
**Key Technology**: Java 21, ASM (bytecode generation), JavaCC (parser), Apache POI (Excel)

---

## Module Architecture

```
┌──────────────────────────────────────────────────┐
│           org.openl.spring                       │
│           (Spring Integration)                   │
├──────────────────────────────────────────────────┤
│           org.openl.rules.test                   │
│           (Testing Framework)                    │
├──────────────────────────────────────────────────┤
│           org.openl.rules.project                │
│           (Project Management)                   │
├──────────────────────────────────────────────────┤
│           org.openl.rules                        │
│           (CORE ENGINE)                          │
│   ┌──────────────────────────────────────┐      │
│   │  Parser → Binder → Codegen → VM      │      │
│   └──────────────────────────────────────┘      │
├──────────────────────────────────────────────────┤
│  org.openl.rules.annotations  │  org.openl.rules.util  │
├──────────────────────────────────────────────────┤
│  org.openl.rules.gen  │  org.openl.rules.constrainer  │
├──────────────────────────────────────────────────┤
│           org.openl.commons                      │
│           (Foundation)                           │
└──────────────────────────────────────────────────┘
```

---

## Submodules Overview

| Module | LOC | Files | Purpose | Dependencies |
|--------|-----|-------|---------|--------------|
| `org.openl.commons` | 3,000+ | 60+ | Foundation utilities, logging | None |
| `org.openl.rules` | 150,000+ | 1,200+ | **Core engine**, parsing, compilation | commons, annotations, util, POI, ASM |
| `org.openl.rules.annotations` | 200 | 5 | Custom annotations | None |
| `org.openl.rules.util` | 5,000+ | 20+ | Built-in rule functions | annotations |
| `org.openl.rules.gen` | 2,000+ | 30+ | Code generation (build-time) | rules, Velocity |
| `org.openl.rules.constrainer` | 10,000+ | 100+ | Constraint solver | commons |
| `org.openl.rules.project` | 15,000+ | 100+ | Project management | rules, JAXB |
| `org.openl.spring` | 3,000+ | 25+ | Spring integration | commons, Spring |
| `org.openl.rules.test` | 500+ | 5+ | Testing framework | rules.project |

---

## 1. org.openl.commons - Foundation

**Location**: `/home/user/openl-tablets/DEV/org.openl.commons/`
**Purpose**: Shared foundation for all OpenL modules

### Key Packages

#### `org.openl.domain` - Domain Modeling
**Purpose**: Represents value domains (sets of allowed values) with type information

**Core Interfaces**:
- **`IDomain<T>`** - Main domain abstraction
  ```java
  interface IDomain<T> {
      IType getElementType();
      boolean selectObject(T obj);
      Iterator<T> iterator();
  }
  ```

**Implementations**:
- `IntRangeDomain` - Integer ranges (e.g., 1..100)
- `DateRangeDomain` - Date ranges
- `EnumDomain` - Enumeration domains
- `IIntDomain` - Integer domain specialization

**Use Case**: Defining valid values for rule parameters (e.g., age must be 18-65)

#### `org.openl.util` - Utility Classes (58 classes)

**Formatters**: `/home/user/openl-tablets/DEV/org.openl.commons/src/org/openl/util/`
- `BooleanFormatter` - Boolean value formatting
- `DateFormatter` - Date/time formatting
- `EnumFormatter` - Enum value formatting
- `NumberFormatter` - Numeric formatting

**Collections**:
- `BiMap` - Bidirectional map
- `CollectionUtils` - Collection helpers
- `ArrayTool`, `ArrayUtils` - Array operations

**File Handling**:
- `FileTool` - File operations
- `FileUtils` - File utilities
- `ZipArchiver`, `ZipUtils` - ZIP compression

**Type Utilities**:
- `ClassUtils` - Class loading and reflection
- `JavaGenericsUtils` - Java generics handling
- `JavaKeywordUtils` - Java keyword checking

**Other**:
- `StringUtils`, `StringTool` - String operations
- `NumberUtils`, `MathUtils` - Math operations
- `SqlDBUtils` - Database utilities
- `JAXBUtils` - XML binding
- `ProjectPackager` - Project packaging

#### `org.openl.info` - Environment Info

**Key Classes**:
- **`OpenLLogger`** - Main static logger initialization
  ```java
  Logger log = OpenLLogger.getLogger(MyClass.class);
  ```
- **`OpenLVersion`** - Version information queries
- **`SysInfo*` classes** - System introspection (JVM, JDBC, JNDI)

### Dependencies

**Internal**: `org.openl.rules.util` (since v6.0.0 - circular dependency)
**External**:
- SLF4J 2.0.17 (logging facade)
- Jakarta XML Bind 4.0.4 (JAXB for Java 11+)

### Entry Points

```java
// Logging
Logger log = OpenLLogger.getLogger(MyClass.class);

// Version info
String version = OpenLVersion.getVersion();

// Class loading
Class<?> clazz = ClassUtils.getClass("com.example.MyClass");
```

---

## 2. org.openl.rules - CORE ENGINE ⭐

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/`
**Purpose**: Complete rules engine - parsing, type system, binding, compilation, execution
**Size**: ~1,200 Java files, 150,000+ LOC

### 2.1 Architecture Overview

```
┌─────────────────────────────────────────────┐
│         EXCEL RULES (Source)                │
└─────────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────┐
│   Parser (BExGrammar - JavaCC)              │
│   Location: org.openl.rules.lang.xls.Parser │
└─────────────────────────────────────────────┘
                   ↓
         ISyntaxNode (Syntax Tree)
                   ↓
┌─────────────────────────────────────────────┐
│   Binder (Type Resolution & Method Binding) │
│   Location: org.openl.binding               │
└─────────────────────────────────────────────┘
                   ↓
         IBoundNode (Compiled AST)
                   ↓
┌─────────────────────────────────────────────┐
│   Code Generation (ASM Bytecode)            │
│   Location: org.openl.rules.runtime         │
└─────────────────────────────────────────────┘
                   ↓
         Generated Proxy Classes
                   ↓
┌─────────────────────────────────────────────┐
│   Virtual Machine (SimpleRulesVM)           │
│   Location: org.openl.vm                    │
└─────────────────────────────────────────────┘
```

### 2.2 Type System

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/types/`

#### Core Type Abstractions

**`IOpenClass`** - Replaces Java `Class`
```java
interface IOpenClass {
    String getName();
    IOpenMethod getMethod(String name, IOpenClass[] params);
    IOpenField getField(String name);
    IOpenField[] getFields();
    IOpenMethod[] getMethods();
    Object newInstance(IRuntimeEnv env);
    boolean isSimple();
    boolean isAbstract();
    boolean isArray();
}
```

**Key Implementations**:
- `AOpenClass` - Abstract base class
- `ADynamicClass` - Dynamic type that can add members at runtime
- `ComponentOpenClass` - Complex types with nested fields
- `StaticOpenClass` - Wraps Java classes
- `NullOpenClass` - Represents null type
- `ArrayOpenClass` - Array types

**Why Not Java Classes?**
- Allows dynamic types created from Excel tables
- Supports runtime type modification
- Enables cross-module type resolution
- Provides unified interface for Java and OpenL types

**`IOpenMethod`** - Method Abstraction
```java
interface IOpenMethod extends IOpenMethodHeader, IMethodCaller {
    IOpenClass getType();                    // Return type
    IMethodSignature getSignature();         // Parameters
    Object invoke(Object target, Object[] params, IRuntimeEnv env);
}
```

**`IOpenField`** - Field Abstraction
```java
interface IOpenField {
    Object get(Object target, IRuntimeEnv env);
    void set(Object target, Object value, IRuntimeEnv env);
    IOpenClass getType();
    String getName();
}
```

### 2.3 Parsing - Excel to Syntax Tree

**Parser Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/lang/xls/Parser.java`
**Grammar**: `/home/user/openl-tablets/DEV/org.openl.rules/grammar/bexgrammar.jj` (JavaCC)

**Parser Methods**:
```java
class Parser implements IOpenParser {
    XlsModuleSyntaxNode parseAsModule(IOpenSourceCodeModule source);
    IdentifierNode[] parseAsParameterDeclaration(String code);
    ISyntaxNode parseAsType(String code);
    ISyntaxNode parseAsMethodHeader(String code);
}
```

**Process**:
1. **Read Excel** using Apache POI
2. **Identify tables** by header patterns (e.g., "Decision Table", "Data", "Spreadsheet")
3. **Parse expressions** in cells using JavaCC grammar
4. **Build syntax tree** (`ISyntaxNode` hierarchy)
5. **Attach metadata** (table properties, cell locations)

**Table Type Detection**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/lang/xls/XlsDefinitions.java`

### 2.4 Binding - Type Resolution

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/binding/`

**Core Interface**:
```java
interface IBoundNode {
    IOpenClass getType();                    // Resolved type
    Object evaluate(IRuntimeEnv env);        // Evaluate at runtime
    void assign(IRuntimeEnv env, Object value); // Assignment
    IBoundNode[] getChildren();
}
```

**Node Binders** (50+ implementations):
- `BinaryOperatorOrNodeBinder` - Binary operations (+, -, *, /, etc.)
- `TypeCastBinder` - Type casting
- `ForNodeBinder` - For loops
- `MethodNodeBinder` - Method calls
- `FieldBoundNode` - Field access
- `LiteralNodeBinder` - Literals (strings, numbers)

**Binding Context**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/binding/IBindingContext.java`
- Manages variable scopes
- Resolves method references
- Handles type conversions
- Tracks compilation errors

**Main Implementation**: `RulesModuleBindingContext` (848 lines) at `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/binding/RulesModuleBindingContext.java`

### 2.5 Decision Tables

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/dt/`

**Core Interface**:
```java
interface IDecisionTable extends IOpenMethod {
    DecisionTableColumnHeaders getColumnHeaders();
    IParameterDeclaration[] getDeclarationParameters();
    // Execution methods
}
```

**Structure**:
```
Decision Table: Calculate Premium
|  Age  |  Risk  | Premium |
|-------|--------|---------|
| 18-25 | High   |  $500   |  <- Rule row
| 26-65 | Low    |  $200   |  <- Rule row
```

**Key Components**:
- **`DecisionTableDataType`** - Represents entire table as a type
- **`DecisionRowField`** - Single rule row
- **`RuleExecutionObject`** - Runtime execution object
- **`DecisionTableColumnHeaders`** - Header metadata

**Algorithm Framework**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/dt/algorithm/`
- `IMatchAlgorithmCompiler` - Compiles matching logic
- `IMatchAlgorithmExecutor` - Executes rule matching
- `WeightAlgorithmCompiler` - Weight-based matching
- `ScoreAlgorithmCompiler` - Score-based matching

**Indexing**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/dt/index/`
- `IRuleIndex` - Indexes rules for fast lookup
- Optimization: Builds search trees for efficient rule matching

**Validation**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/dt/validator/`
- `DecisionTableValidator` - Validates completeness
- `DecisionTableAnalyzer` - Detects overlaps and gaps
- `DecisionTableUncovered` - Uncovered scenarios
- `DecisionTableOverlapping` - Overlapping rules

### 2.6 Data Tables

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/data/`

**Purpose**: Store structured data in Excel tables

**Example**:
```
Datatype Rate
| State | Rate  |
|-------|-------|
| CA    | 0.075 |
| NY    | 0.085 |
| TX    | 0.060 |
```

**Key Classes**:
- **`ITable`** - Table data interface
- **`ITableModel`** - Grid-based model
- **`DataOpenField`** - Field in data table
- **`OpenlBasedDataTableModel`** - OpenL implementation
- **`FieldChain`** - Navigate complex objects

### 2.7 Spreadsheet Tables

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/calc/`

**Purpose**: Excel-like grid calculations

**Key Classes**:
- **`Spreadsheet`** - Main spreadsheet type
- **`SpreadsheetInvoker`** - Invocation handler
- **`SpreadsheetStructureBuilder`** - Builds structure from Excel

**Features**:
- Cell formulas
- Cross-cell references
- Calculated columns
- Aggregate functions

### 2.8 Datatype Tables

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/datatype/`

**Purpose**: Define custom business types in Excel

**Example**:
```
Datatype Person
| Field Name | Type   |
|------------|--------|
| name       | String |
| age        | Integer|
| salary     | Double |
```

**Code Generation**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/datatype/gen/`
- **`JavaBeanClassBuilder`** - Generates Java classes using ASM
- **`ASMUtils`** - ASM bytecode utilities
- **`FieldDescriptionBuilder`** - Field metadata

**Process**:
1. Parse datatype table
2. Build `IOpenClass` dynamically
3. Generate bytecode with getters/setters
4. Load into JVM

### 2.9 Column Match Tables

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/cmatch/`

**Purpose**: Specialized matching algorithm for complex scenarios

**Key Classes**:
- **`ColumnMatch`** - Main type
- **`ColumnMatchBoundNode`** - Compiled node
- **Matchers**:
  - `EnumMatchMatcher` - Enum matching
  - `ClassMatchMatcher` - Class matching
  - `NumberMatchMatcher` - Numeric matching

### 2.10 Algorithm Tables (TBasic)

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/tbasic/`

**Purpose**: Imperative algorithm definitions

**Key Classes**:
- **`AlgorithmInvoker`** - Executes algorithms
- **`AlgorithmTableParserManager`** - Parses algorithm syntax
- **Compile/Runtime packages**: Compilation and execution logic

### 2.11 Test Methods

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/testmethod/`

**Purpose**: Define test cases in Excel

**Key Classes**:
- **`TestSuiteMethod`** - Test execution
- **`TestUnitsResults`** - Test results
- **`RulesResultExport`** - Export test results

### 2.12 Compilation & Execution

**Compilation Entry Point**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/engine/OpenLCompileManager.java`

```java
CompiledOpenClass compile(IOpenSourceCodeModule source);
```

**Runtime Entry Point**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/runtime/RulesEngineFactory.java`

```java
RulesEngineFactory<T> factory =
    new RulesEngineFactory<>(sourceFile, interfaceClass);

T instance = factory.newInstance();
```

**Execution Flow**:
1. **Proxy Generation** - ASM generates proxy class implementing interface
2. **Method Interception** - `OpenLRulesMethodHandler` intercepts calls
3. **Context Injection** - `IRulesRuntimeContext` provides execution context
4. **VM Execution** - `SimpleRulesVM` executes compiled code
5. **Result Return** - Result returned to caller

**Runtime Context**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/context/`
- **`IRulesRuntimeContext`** - Holds execution state (date, region, etc.)
- **`DefaultRulesRuntimeContext`** - Default implementation
- **Use Case**: Rule versioning by context dimensions

### 2.13 Validation Framework

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/validation/`

**Key Classes**:
- **`ValidationManager`** - Orchestrates validation
- **`IOpenLValidator`** - Validator interface
- **Property Validation**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/rules/validation/properties/`
  - Dimensional validation
  - Constraint checking
  - Property consistency

### 2.14 Error Handling

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/exception/`

**Exception Hierarchy**:
```
OpenLException
├── OpenLCompilationException    (compile-time)
├── OpenLRuntimeException        (runtime)
└── OpenLUserRuntimeException    (user-level runtime)
```

**Message System**: `/home/user/openl-tablets/DEV/org.openl.rules/src/org/openl/message/`
- **`OpenLMessage`** - Structured message with severity
- **`OpenLErrorMessage`** - Error messages
- **`OpenLWarnMessage`** - Warning messages
- **`OpenLMessagesUtils`** - Message utilities

### Dependencies

**External**:
- Apache POI 5.4.1 (Excel parsing) - CRITICAL
- ASM 9.8 (Bytecode generation) - CRITICAL
- Apache Commons Lang3 3.19.0
- Apache Commons Collections4 4.5.0
- Groovy 4.0.28 (expression evaluation)
- Jakarta XML Bind 4.0.4

**Internal**:
- `org.openl.commons`
- `org.openl.rules.annotations`
- `org.openl.rules.util`
- `org.openl.rules.constrainer`

### Entry Points Reference

| Use Case | Entry Point | Location |
|----------|-------------|----------|
| Compile single file | `RulesEngineFactory<T>` | `org.openl.rules.runtime.RulesEngineFactory` |
| Parse Excel | `Parser.parseAsModule()` | `org.openl.rules.lang.xls.Parser` |
| Programmatic compilation | `OpenLManager.makeMethod()` | `org.openl.engine.OpenLManager` |
| Execute rules | `factory.newInstance()` | (generated proxy) |

---

## 3. org.openl.rules.annotations

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules.annotations/`
**Purpose**: Custom annotations for OpenL extensions
**Size**: 5 annotation classes

### Annotations

**`@Operator`** - Define Custom Operators
```java
@Operator
public static int add(int a, int b) {
    return a + b;
}
```
**Location**: `org.openl.rules.annotations.Operator`
**Target**: TYPE, METHOD
**Retention**: RUNTIME
**Use**: Registers operators in `LibrariesRegistry`

**`@ContextProperty`** - Mark Context Properties
```java
public class Context {
    @ContextProperty("region")
    private String region;
}
```
**Use**: Enables rule versioning by context dimensions

**`@NonNullLiteral`** - Disallow Null Values
**Use**: Literal value validation

**`@IgnoreNonVarargsMatching`** / **`@IgnoreVarargsMatching`**
**Use**: Control method matching for varargs methods

### Dependencies
None - only standard Java annotations

---

## 4. org.openl.rules.util

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules.util/`
**Purpose**: Built-in functions callable from OpenL rules

### Utility Classes

**Collections**:
- `Arrays` - Array operations (filter, map, reduce)
- `Strings` - String manipulations
- `Numbers` - Numeric operations
- `Booleans` - Boolean utilities
- `Dates` - Date/time utilities

**Aggregations**:
- `Sum`, `Avg`, `Product` - Statistical functions
- `Statistics` - General statistics

**Math**:
- `Round` - Rounding operations

**Miscellaneous**:
- `Miscs` - General utilities

**Date Intervals**: `/home/user/openl-tablets/DEV/org.openl.rules.util/src/org/openl/rules/util/dates/`
- `DateInterval` - Date range abstraction
- `DateIntervalImpl` - Implementation
- `CalendarWrapper` - Calendar utilities

### Usage Example

In Excel rules, these functions are available:
```
Sum(prices)              // Sum array
Avg(ages)               // Average
Round(value, 2)         // Round to 2 decimals
Strings.join(list, ",") // Join strings
```

### Dependencies
- `org.openl.rules.annotations` (uses `@Operator`)

---

## 5. org.openl.rules.gen - Code Generation

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules.gen/`
**Type**: POM project (not deployed)
**Purpose**: Generates code at build time

### Tools

**Main Generators**:
- `GenRulesCode` - Generates rule execution code
- `GenRulesTypes` - Generates type definitions
- `SourceGenerator` - Abstract generator
- `VelocityTool` - Template processor

**Velocity Templates** (12 templates):
1. `ITableProperties-properties.vm` - Interface properties
2. `DefaultTableProperties-properties.vm` - Default properties
3. `DefaultRulesContext-properties.vm` - Context properties
4. `DefaultPropertyDefinitions.vm` - Definitions
5. `TableProperties-properties.vm` - Table properties
6. `MatchingOpenMethodDispatcher.vm` - Method dispatch
7. `DefaultPropertiesIntersectionFinder.vm` - Property intersection
8. `DefaultPropertiesContextMatcher-constraints.vm` - Context matching
9. `DefaultTablePropertiesSorter-constraints.vm` - Sorting
10. `RulesCompileContext-validators.vm` - Validation code
11. `rules-enum.vm` - Enum generation
12. `IRulesContext-properties.vm` - Context interface

### Process
1. Read table property definitions
2. Apply Velocity templates
3. Generate Java source files
4. Compile into engine

### Dependencies
- `org.openl.rules` (engine)
- Apache Velocity 2.4.1

**Note**: Only used during build, not deployed to runtime

---

## 6. org.openl.rules.constrainer - Constraint Solver

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules.constrainer/`
**Purpose**: Constraint programming engine for CSP (Constraint Satisfaction Problems)

### Core Abstractions

**`Constrainer`** - Main solver engine
```java
Constrainer constrainer = new Constrainer();
IntVar x = constrainer.addIntVar(0, 10, "x");
constrainer.addConstraint(x.gt(5));
boolean solved = constrainer.findSolution();
```

**`IntVar`** - Integer variable
- Domain - Value domain
- Operations: `add()`, `mul()`, `lt()`, `gt()`, etc.

**`IntExp`** - Integer expression
- `IntBoolExp` - Boolean expressions
- `IntExpConst` - Constants

**`Constraint`** - Abstract constraint
- `ConstraintConst` - Constant constraints

**`Goal`** - Search goal
- `GoalSetMin`, `GoalSetMax` - Optimization goals
- `GoalOr` - OR goals

### Execution Model
- **Observer Pattern** - Constraint propagation
- **Backtracking** - `ChoicePointLabel`
- **Undo** - Rollback mechanism

### Use Case
Rarely used in typical OpenL projects, but available for:
- Resource allocation
- Scheduling problems
- Optimization scenarios
- Complex constraint satisfaction

### Dependencies
- `org.openl.commons` only

---

## 7. org.openl.rules.project - Project Management

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules.project/`
**Purpose**: Manages OpenL projects - loading, configuration, instantiation
**Size**: ~100 Java files, 15,000+ LOC

### 7.1 Project Model

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules.project/src/org/openl/rules/project/model/`

**`ProjectDescriptor`** - Main configuration (848 lines)
```java
@XmlRootElement
class ProjectDescriptor {
    String name;
    List<Module> modules;
    List<PathEntry> classpath;
    List<ProjectDependencyDescriptor> dependencies;
    OpenAPI openapi;

    // Methods
    File getProjectFolder();
    URL[] getClassPathUrls();
    URI getRelativeUri(File file);
}
```
**File**: `rules.xml` (project root)

**Example `rules.xml`**:
```xml
<project>
    <name>Insurance Rules</name>
    <modules>
        <module>
            <name>premium-calc</name>
            <rules-root path="rules/premium"/>
        </module>
    </modules>
    <dependencies>
        <dependency>
            <name>common-rules</name>
        </dependency>
    </dependencies>
</project>
```

**`Module`** - Single module definition
```java
class Module {
    String name;
    PathEntry rulesSource;
    PathEntry testSource;
}
```

**`PathEntry`** - Classpath entry (URL or file)

**`ProjectDependencyDescriptor`** - External project dependency

### 7.2 Project Serialization

**`XmlProjectDescriptorSerializer`** - JAXB-based XML serialization
- Reads/writes `rules.xml`
- Tag constants: `PROJECT_DESCRIPTOR_TAG`, `DEPENDENCY_TAG`
- Custom adapters for whitespace handling

### 7.3 Project Instantiation (CRITICAL)

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules.project/src/org/openl/rules/project/instantiation/`

**`SimpleProjectEngineFactory<T>`** - Main factory

**Builder Pattern**:
```java
SimpleProjectEngineFactory<MyRules> factory =
    new SimpleProjectEngineFactoryBuilder<MyRules>()
        .setProject("/path/to/project")
        .setInterfaceClass(MyRules.class)
        .setExecutionMode(true)
        .build();

MyRules instance = factory.newInstance();
```

**Configuration Methods**:
- `setProject(String)` - Project directory
- `setWorkspace(String)` - Workspace path
- `setInterfaceClass(Class<T>)` - Service interface
- `setClassLoader(ClassLoader)` - Custom classloader
- `setExecutionMode(boolean)` - Execution vs compilation
- `setExternalParameters(Map)` - Parameters
- `setProjectDependencies(String...)` - Dependencies

**Instantiation Strategies**:
- `RulesInstantiationStrategy` - Base interface
- `CommonRulesInstantiationStrategy` - Common impl
- `SimpleMultiModuleInstantiationStrategy` - Multi-module
- `ApiBasedInstantiationStrategy` - API-based

**Dependency Management**:
- `IDependencyLoader` - Loads dependencies
- `SimpleDependencyLoader` - Simple implementation
- `AbstractDependencyManager` - Base manager
- `RuntimeContextInstantiationStrategyEnhancer` - Context injection

### 7.4 Project Resolution

**`ProjectResolver`** - Finds projects in filesystem
**`ProjectResourceLoader`** - Loads project resources
**`ResolvingStrategy`** - Resource discovery strategies
- `SimpleXlsResolvingStrategy` - Simple Excel discovery
- `ProjectDescriptorBasedResolvingStrategy` - Descriptor-based

### 7.5 File Name Processing

**`PropertiesFileNameProcessor`** - Processes properties files
**`DefaultPropertiesFileNameProcessor`** - Default implementation

**Purpose**: Parse file names with embedded properties
Example: `premium-calc%region=US%date=2024.xlsx`

### Dependencies
- `org.openl.rules` (engine)
- Apache Commons Lang3 3.19.0
- JAXB Runtime 4.0.4

### Entry Points

**Fluent Builder**:
```java
new SimpleProjectEngineFactoryBuilder<T>()
    .setProject("path")
    .build()
```

**Direct**:
```java
new SimpleProjectEngineFactory<>(projectPath, interfaceClass)
```

---

## 8. org.openl.spring - Spring Integration

**Location**: `/home/user/openl-tablets/DEV/org.openl.spring/`
**Purpose**: Spring Framework integration for OpenL
**Size**: ~25 classes, 3,000+ LOC

### 8.1 Conditional Configuration

**`@ConditionalOnEnable`** - Conditional bean registration

```java
@Configuration
@ConditionalOnEnable({"feature.enabled", "module.enabled"})
public class MyConfig {
    // Beans only registered if properties are non-false/non-empty
}
```

**Implementation**: `EnableCondition` evaluates properties

### 8.2 Property Sources Framework

**Location**: `/home/user/openl-tablets/DEV/org.openl.spring/src/org/openl/spring/env/`

**Base Classes**:
- `DynamicPropertySource` - Base for dynamic properties
- `PropertySourcesLoader` - Loads property sources
- `ApplicationPropertySource` - Application-level
- `DefaultPropertySource` - Default properties

**Specialized Sources**:
- `SysPropPropertySource` - System properties
- `SysEnvRefPropertySource` - Environment variables
- `SysInfoPropertySource` - OS/JVM info
- `ServletContextPropertySource` - Servlet context
- `RandomValuePropertySource` - Random values
- `RefPropertySource` - Property references (`${}`)

**Features**:
- **Property References**: `${other.property}`
- **Security**: `PassCoder` for password encoding
- **Firewall**: `FirewallPropertyResolver` for security
- **Multi-source**: Hierarchical property resolution

### Usage Example

```java
@Configuration
public class AppConfig {
    @Autowired
    private Environment env;

    @Bean
    public DataSource dataSource() {
        String url = env.getProperty("db.url");
        // Supports: db.url=${default.db.url}
    }
}
```

### Dependencies
- Spring Beans 6.2.11
- Spring Context 6.2.11
- Jakarta Servlet API 6.0.0
- `org.openl.commons`

---

## 9. org.openl.rules.test - Testing Framework

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules.test/`
**Purpose**: Testing framework for OpenL rules
**Size**: ~5 classes, 500+ LOC

### Main Component

**`RulesInFolderTestRunner`** - Executes tests from folder

**Usage**:
```java
RulesInFolderTestRunner runner =
    new RulesInFolderTestRunner("/path/to/project");
TestResults results = runner.run();
```

**Features**:
- Discovers test files automatically
- Executes test methods
- Reports results (pass/fail)
- Integration with project system

### Test Resources

Located in `/test-resources/`:
- Test fixtures
- Multi-module test projects
- Dependency scenarios
- Properties file patterns

### Dependencies
- `org.openl.rules.project` (project instantiation)

---

## Module Dependencies Graph

```
                    ┌─────────────────┐
                    │  org.openl.     │
                    │    spring       │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  org.openl.     │
                    │  rules.test     │
                    └────────┬────────┘
                             │
         ┌───────────────────▼────────────────────┐
         │      org.openl.rules.project           │
         └───────────────────┬────────────────────┘
                             │
         ┌───────────────────▼────────────────────┐
         │         org.openl.rules                │
         │           (CORE ENGINE)                │
         └───┬──────┬──────────────┬──────────────┘
             │      │              │
    ┌────────▼──┐ ┌▼──────────┐  ┌▼────────────────┐
    │org.openl. │ │org.openl. │  │org.openl.rules. │
    │  rules.   │ │  rules.   │  │  constrainer    │
    │annotations│ │   util    │  └─────────────────┘
    └───────────┘ └───────────┘
             │      │
             └──────┼──────────┐
                    │          │
             ┌──────▼──────┐  ┌▼──────────────┐
             │org.openl.   │  │org.openl.rules│
             │  commons    │  │     .gen      │
             └─────────────┘  └───────────────┘
```

---

## Critical Paths & Entry Points

### Compilation Path

```
Source File
    ↓
RulesEngineFactory<T>
    ↓
OpenLCompileManager
    ↓
Parser.parseAsModule()
    ↓
Syntax Tree (ISyntaxNode)
    ↓
Binder.bind()
    ↓
Bound Tree (IBoundNode)
    ↓
ASM Bytecode Generation
    ↓
CompiledOpenClass
```

### Execution Path

```
factory.newInstance()
    ↓
ASM Proxy Class
    ↓
OpenLRulesMethodHandler
    ↓
Method Lookup
    ↓
SimpleRulesVM.execute()
    ↓
IRuntimeEnv
    ↓
Result
```

### Project Loading Path

```
SimpleProjectEngineFactory
    ↓
ProjectDescriptor (rules.xml)
    ↓
Module Resolution
    ↓
Dependency Loading
    ↓
Multi-Module Compilation
    ↓
Instantiation Strategy
    ↓
Generated Proxy
```

---

## Known Issues & Technical Debt

### Deprecated Code

1. **`OpenL.getInstance()`** - Marked with TODO: DO NOT USE
   - **Issue**: Static singleton anti-pattern
   - **Status**: Needs refactoring

2. **Operator Methods** - 20+ deprecated in `Operators` class
   - **Issue**: Should use annotation-based registration
   - **Status**: Planned removal

3. **Source Modules** - Several `*SourceCodeModule` classes deprecated
   - **Issue**: Replaced with simpler abstractions
   - **Status**: Scheduled for removal

### TODOs & FIXMEs (30+ identified)

**Architecture Issues**:
- Method calling in constructor - `ComponentOpenClass:52`
- Generic type support missing - `CastToWiderType:78`
- Array dimension initialization - `NewArrayNodeBinder:45`
- Static method access design - `TypeBoundNode:120`

**Code Quality**:
- String interning review - `RuleRowHelper:234`
- Cache implementation (memory leak risk) - `JavaOpenClassCache:156`
- Public field refactoring - `BinaryNode`, `UnaryNode`
- Legacy method search - `MethodSearch:445`

**Feature Gaps**:
- Security blocking for system classes - `TypeResolver:89`
- Excel format consideration - `RuleRowHelper:456`
- Comma-separated array parsing - `RuleRowHelper:523`
- URL space support - `URLSourceCodeModule:67`

### Excluded Tests

In `org.openl.rules.constrainer/pom.xml`:
- `TestExecutionControl`
- `TestExecutionControl2`
- `TestIntExpElementAt`

**Status**: Excluded from Surefire - likely broken or legacy

---

## Performance Considerations

### Compilation Performance

**Bottlenecks**:
1. Excel parsing (Apache POI)
2. Type resolution (large type hierarchies)
3. Method binding (overload resolution)
4. Bytecode generation (ASM)

**Optimizations**:
- Rule indexing for fast lookup
- Caching of compiled classes
- Lazy loading of modules

### Execution Performance

**Optimizations**:
1. Bytecode generation (native JVM performance)
2. Method dispatch optimization
3. Context caching
4. Rule indexing

**Profiling**: Use `org.openl.rules.profiler` module

---

## Testing Strategy

### Unit Tests
- JUnit 5 framework
- Mockito for mocking
- Test each module independently

### Integration Tests
- Located in `/ITEST/`
- TestContainers for Docker-based tests
- End-to-end scenarios

### Test Coverage
- Core engine: High (>80%)
- Project management: Medium (>60%)
- Utilities: High (>80%)

---

## External Dependencies Summary

### Build-Time
- **JavaCC** - Parser generation
- **Maven** - Build system

### Compile-Time & Runtime
- **ASM 9.8** - Bytecode generation (CRITICAL)
- **Apache POI 5.4.1** - Excel parsing (CRITICAL)
- **Groovy 4.0.28** - Expression evaluation
- **Apache Commons** - Lang3, Collections4
- **SLF4J 2.0.17** - Logging
- **Jakarta XML Bind 4.0.4** - JAXB
- **Velocity 2.4.1** - Templates (code gen)
- **Spring 6.2.11** - Spring integration

### Test-Time
- **JUnit 5** - Testing
- **Mockito** - Mocking
- **Spring Test** - Spring testing

---

## See Also

- [Technology Stack](/docs/architecture/technology-stack.md) - Complete tech stack
- [Codebase Tour](/docs/onboarding/codebase-tour.md) - Navigate the codebase
- [Development Setup](/docs/onboarding/development-setup.md) - Get started
- [Root CLAUDE.md](/CLAUDE.md) - Coding conventions
- [DEV/CLAUDE.md](/DEV/CLAUDE.md) - Core engine conventions

---

**Module Documentation Complete**
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-05

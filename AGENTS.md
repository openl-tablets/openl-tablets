# OpenL Tablets - Claude Code Conventions

**Repository**: OpenL Tablets
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-05

---

## Purpose

This file provides guidelines for Claude Code when working with the OpenL Tablets codebase. It documents coding conventions, architecture principles, common patterns, and areas requiring special attention.

---

## Repository Overview

OpenL Tablets is an enterprise business rules engine that compiles Excel spreadsheets into executable Java bytecode.

**Key Characteristics**:
- **Multi-module Maven project** (50+ modules)
- **Java 21+** required
- **Complex architecture** with parsing, type system, bytecode generation
- **Dual frontend**: Legacy JSF/RichFaces + Modern React/TypeScript
- **Production-grade**: Used in enterprise environments

---

## Module Structure

### Main Module Groups

| Module | Purpose | Technology | Modernization Status |
|--------|---------|------------|---------------------|
| **DEV** | Core rules engine | Java, ASM, JavaCC | ✅ Modern |
| **STUDIO** | Web IDE | Spring Boot, React, JSF (legacy) | ⚠️ Migrating to React |
| **WSFrontend** | Rule services | Spring Boot, CXF | ✅ Modern |
| **Util** | Tools & utilities | Java, Maven | ✅ Modern |
| **ITEST** | Integration tests | JUnit 5, TestContainers | ✅ Modern |

### Module-Specific Conventions

Each major module has its own `AGENTS.md` file with specific conventions:
- `/DEV/AGENTS.md` - Core engine conventions
- `/STUDIO/AGENTS.md` - Studio conventions (when created)
- `/STUDIO/studio-ui/AGENTS.md` - React/TypeScript conventions (when created)
- `/WSFrontend/AGENTS.md` - Web services conventions (when created)

**Always check module-specific AGENTS.md files before making changes.**

---

## Coding Standards

### Java

**Version**: Java 21+
**Style**: Standard Java conventions

#### Key Conventions

1. **Package Structure**:
   ```text
   org.openl.<module>.<feature>
   Example: org.openl.rules.dt (decision tables)
   ```

2. **Class Naming**:
    - Interfaces: `I<Name>` (e.g., `IOpenClass`, `IOpenMethod`)
    - Abstract classes: `A<Name>` or `<Name>Base` (e.g., `AOpenClass`)
    - Implementations: Descriptive names (e.g., `StaticOpenClass`, `DynamicOpenClass`)

3. **Method Naming**:
    - Standard Java camelCase
    - Boolean methods: `is*()`, `has*()`, `can*()`
    - Getters/setters: Standard JavaBean conventions

4. **Comments**:
    - JavaDoc for public APIs
    - `TODO:` for planned work
    - `FIXME:` for known issues
    - `@Deprecated` for deprecated code with migration path

5. **Annotations**:
    - Use `@Override` consistently
    - `@Deprecated` with JavaDoc explaining alternative
    - `@SuppressWarnings` sparingly with justification

#### Example Pattern

```java
/**
 * Represents an OpenL type (equivalent to Java Class).
 *
 * @author OpenL Tablets
 */
public interface IOpenClass {
    /**
     * Gets the type name.
     *
     * @return type name, never null
     */
    String getName();

    /**
     * Creates a new instance of this type.
     *
     * @param env runtime environment
     * @return new instance
     * @throws OpenLRuntimeException if instantiation fails
     */
    Object newInstance(IRuntimeEnv env);
}
```

### TypeScript/React (Frontend)

**Version**: TypeScript 5.9.x, React 19.2.x
**Style**: Standard TS/React conventions

#### Key Conventions

1. **Component Structure**:
   ```typescript
   // Functional components with hooks
   export const MyComponent: React.FC<MyComponentProps> = ({ prop1, prop2 }) => {
       const [state, setState] = useState<StateType>(initialValue);
       // ...
   };
   ```

2. **File Naming**:
    - Components: PascalCase (e.g., `RuleEditor.tsx`)
    - Utilities: camelCase (e.g., `formatDate.ts`)
    - Types: PascalCase (e.g., `types.ts` with exported types)

3. **State Management**:
    - Use Zustand for global state
    - Use React hooks for local state
    - Keep state close to where it's used

4. **Imports**:
    - Absolute imports from `src/`
    - Group imports: React → Third-party → Local

### Maven

1. **Dependency Management**:
    - Use properties for versions in root POM
    - Manage dependencies in `<dependencyManagement>`
    - Avoid version overrides in child POMs

2. **Build Profiles**:
    - Use existing profiles (`quick`, `skipTests`, `noDocker`)
    - Document new profiles in root POM

---

## Architecture Principles

### 1. Layered Architecture

OpenL follows a strict layered architecture:

```text
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

**Rule**: Lower layers should not depend on higher layers.

### 2. Type System

OpenL has its own type system parallel to Java:
- `IOpenClass` (not Java `Class`)
- `IOpenMethod` (not Java `Method`)
- `IOpenField` (not Java `Field`)

**Why?**: Enables dynamic types from Excel tables.

**Rule**: Always use OpenL types internally; convert to/from Java types at boundaries.

### 3. Binding vs. Execution

Clear separation:
- **Compile-time (Binding)**: Type checking, method resolution, optimization
- **Runtime (Execution)**: Actual execution in VM

**Rule**: Never mix compile-time logic into runtime execution paths.

### 4. Extension Points

The system is designed for extensibility:
- **Node Binders**: Add new syntax support
- **Type Providers**: Add new type sources
- **Table Types**: Add new table types
- **Instantiation Strategies**: Custom project loading

**Rule**: Use existing extension points rather than modifying core classes.

---

## Common Patterns

### 1. Factory Pattern

Used extensively for object creation:

```java
// Engine factory
RulesEngineFactory<MyRules> factory =
    new RulesEngineFactory<>("rules.xlsx", MyRules.class);

// Project factory
SimpleProjectEngineFactory<MyRules> factory =
    new SimpleProjectEngineFactoryBuilder<MyRules>()
        .setProject("path")
        .setInterfaceClass(MyRules.class)
        .build();
```

### 2. Visitor Pattern

For traversing syntax trees:

```java
interface ISyntaxNode {
    void accept(ISyntaxNodeVisitor visitor);
}

class MyVisitor implements ISyntaxNodeVisitor {
    public void visit(ISyntaxNode node) {
        // Process node
    }
}
```

### 3. Strategy Pattern

For pluggable algorithms:

```java
interface RulesInstantiationStrategy {
    Object instantiate(CompiledOpenClass compiledClass);
}

// Implementations: ApiBasedInstantiationStrategy, CommonRulesInstantiationStrategy
```

### 4. Builder Pattern

For complex object construction:

```java
SimpleProjectEngineFactory<T> factory =
    new SimpleProjectEngineFactoryBuilder<T>()
        .setProject(projectPath)
        .setInterfaceClass(interfaceClass)
        .setExecutionMode(true)
        .build();
```

---

## Critical Areas - Handle with Care

### 1. Core Type System

**Location**: `/DEV/org.openl.rules/src/org/openl/types/`

**Why Critical**: Foundation of entire engine
**Rules**:
- Never break `IOpenClass`, `IOpenMethod`, `IOpenField` contracts
- Changes ripple through entire codebase
- Test exhaustively

### 2. Parser & Grammar

**Location**: `/DEV/org.openl.rules/grammar/bexgrammar.jj`

**Why Critical**: Defines language syntax
**Rules**:
- Requires JavaCC knowledge
- Grammar changes affect all rules
- Maintain backward compatibility
- Extensive regression testing required

### 3. Bytecode Generation

**Location**: `/DEV/org.openl.rules/src/org/openl/rules/runtime/`

**Why Critical**: Generates executable code
**Rules**:
- Requires ASM library expertise
- Errors cause runtime failures
- Verify with ASMifier for correct bytecode
- Test with various JVM versions

### 4. Binding System

**Location**: `/DEV/org.openl.rules/src/org/openl/binding/`

**Why Critical**: Type resolution and method binding
**Rules**:
- Complex overload resolution logic
- Performance-sensitive
- Handle type conversions carefully

---

## Legacy Code Areas

### 1. JSF/RichFaces UI

**Location**: `/STUDIO/org.openl.rules.webstudio/src/main/webapp/`

**Status**: ⚠️ **Being replaced by React** - Do not enhance, only bug fixes
**Notes**:
- RichFaces is a forked project maintained by OpenL
- Migration to React in progress
- New features go in `studio-ui` React app

### 2. Deprecated APIs

**Markers**:
- `@Deprecated` annotation
- `TODO: DO NOT USE` comments
- `FIXME:` comments

**Examples**:
- `OpenL.getInstance()` - Static singleton anti-pattern
- Legacy source code modules - Replaced with simpler abstractions
- Deprecated operator methods - Use annotation-based instead

**Rule**: Do not use deprecated APIs; migrate to alternatives.

### 3. Excluded Tests

**Location**: Various `pom.xml` files with `<excludes>`

**Examples**:
- `org.openl.rules.constrainer`: `TestExecutionControl*`

**Status**: Known broken tests, excluded from build
**Rule**: Fix if possible, document if not

---

## Technical Debt

### Known Issues

1. **Memory Leaks**:
    - Cache implementation in `JavaOpenClassCache` (needs review)
    - String interning in `RuleRowHelper` (needs review)

2. **Architecture Issues**:
    - Method calling in constructor: `ComponentOpenClass`
    - Public fields in `BinaryNode`, `UnaryNode` (should be private)
    - Static method access design in `TypeBoundNode`

3. **Feature Gaps**:
    - Security blocking for system class access: `TypeResolver`
    - Generic type support missing: `CastToWiderType`
    - Array dimension initialization: `NewArrayNodeBinder`

**Rule**: When fixing bugs in these areas, consider addressing technical debt.

---

## Testing Requirements

### Unit Tests

**Framework**: JUnit 5
**Mocking**: Mockito

**Requirements**:
- Test public APIs thoroughly
- Use descriptive test method names: `testMethodName_scenario_expectedResult`
- Organize tests by feature
- Include edge cases

**Example**:
```java
@Test
void testParseAsModule_validExcel_returnsModuleSyntaxNode() {
    // Arrange
    IOpenSourceCodeModule source = ...;

    // Act
    XlsModuleSyntaxNode result = parser.parseAsModule(source);

    // Assert
    assertNotNull(result);
    assertEquals("MyModule", result.getName());
}
```

### Integration Tests

**Location**: `/ITEST/`
**Framework**: TestContainers, Spring Test

**Requirements**:
- Test end-to-end scenarios
- Use Docker containers for external dependencies
- Clean up resources

### Running Tests

```bash
# All tests
mvn test

# Quick tests only
mvn test -Dquick

# Specific test
mvn test -Dtest=MyTest

# Integration tests
cd ITEST/itest.smoke
mvn verify
```

---

## Build Guidelines

### Build Commands

```bash
# Full build
mvn clean install

# Quick build (recommended for development)
mvn clean install -Dquick -DnoPerf -T1C

# Skip tests
mvn clean install -DskipTests

# Skip Docker tests
mvn clean install -DnoDocker
```

### Build Profiles

| Profile | Purpose |
|---------|---------|
| `quick` | Skip heavy tests |
| `skipTests` | Skip all tests |
| `noPerf` | No memory limits on tests |
| `noDocker` | Skip Docker-based tests |
| `sonar` | SonarQube analysis |
| `owasp` | Security vulnerability scan |

### Multi-Module Builds

When modifying dependencies:

```bash
# Build from root to ensure consistency
cd /path/to/openl-tablets
mvn clean install -DskipTests

# Or build specific module tree
cd DEV
mvn clean install -DskipTests
```

---

## Git Workflow

### Branch Naming

- Feature branches: `feature/<description>`
- Bug fixes: `fix/<issue-number>`
- Claude Code branches: `claude/<description>-<session-id>`

### Commit Messages

**Format**:
```text
<type>: <subject>

<body>

<footer>
```

**Types**: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`

**Example**:
```text
feat: Add support for nested datatype tables

Implement parsing and binding for nested datatype definitions.
This allows creating hierarchical data structures in Excel.

Closes #1234
```

### Pre-Commit Checks

Before committing:
1. ✅ Build succeeds: `mvn clean install -Dquick`
2. ✅ Tests pass: `mvn test`
3. ✅ No new warnings
4. ✅ Code formatted correctly

---

## Common Tasks

### Task: Add a New Table Type

**Steps**:
1. Define table syntax in Excel
2. Create node binder in `/DEV/org.openl.rules/src/org/openl/rules/lang/xls/binding/`
3. Create bound node in `/DEV/org.openl.rules/src/org/openl/rules/binding/`
4. Register in `XlsDefinitions`
5. Add unit tests
6. Add integration tests
7. Document in user guide

### Task: Add REST Endpoint

**Steps**:
1. Create `@RestController` in appropriate module
2. Implement endpoint with proper error handling
3. Add OpenAPI annotations
4. Write unit tests
5. Write integration tests
6. Update API documentation

### Task: Add UI Feature (React)

**Steps**:
1. Create component in `/STUDIO/studio-ui/src/components/`
2. Add routing if needed (React Router)
3. Connect to backend API
4. Add i18n translations
5. Write unit tests (Jest/React Testing Library)
6. Test in browser

### Task: Fix a Bug

**Steps**:
1. Reproduce the bug with a test
2. Identify root cause
3. Fix the issue
4. Ensure test passes
5. Run related tests
6. Document fix in commit message

---

## Performance Considerations

### Compilation Performance

**Bottlenecks**:
- Excel parsing (POI)
- Type resolution
- Method binding
- Bytecode generation

**Optimization Tips**:
- Use rule indexing for large decision tables
- Cache compiled classes
- Lazy-load modules when possible

### Runtime Performance

**Optimization**:
- Bytecode generation provides native JVM performance
- Use method dispatch optimization
- Cache runtime contexts
- Profile with `org.openl.rules.profiler`

**Avoid**:
- Reflection in hot paths
- Unnecessary object creation
- Synchronization where not needed

---

## Security Considerations

### Input Validation

- Validate all user inputs
- Sanitize file paths
- Check file sizes/types before processing

### Authentication/Authorization

- Respect security framework
- Use Spring Security annotations
- Test permission checks

### Sensitive Data

- Never log passwords or tokens
- Use `PassCoder` for password encoding
- Follow OWASP guidelines

---

## Documentation

### Code Documentation

- JavaDoc for all public APIs
- Explain "why" not just "what"
- Include usage examples for complex APIs
- Document side effects and thread safety

### Architecture Documentation

**Location**: `/Docs/`

When making architectural changes:
1. Update relevant docs in `/Docs/architecture/`
2. Update module analysis docs in `/Docs/analysis/`
3. Update migration status if applicable

---

## Resources

### Documentation

- **Architecture**: `/Docs/architecture/` - System architecture
- **Onboarding**: `/Docs/onboarding/` - Getting started guides
- **Analysis**: `/Docs/analysis/` - Module deep dives
- **Plans**: `/Docs/plans/` - Current work and technical debt
- **Project Docs**: `/Docs/` - Existing project documentation

### Key Files

| File | Purpose |
|------|---------|
| `/README.md` | Build instructions |
| `/pom.xml` | Root Maven configuration |
| `/docker-compose.yaml` | Docker setup |
| `/Docs/Configuration.md` | Configuration options |
| `/Docs/Production_Deployment.md` | Production deployment |

### External Resources

- **Website**: [https://openl-tablets.org](https://openl-tablets.org)
- **GitHub**: [https://github.com/openl-tablets/openl-tablets](https://github.com/openl-tablets/openl-tablets)
- **Maven Central**: `org.openl` group ID

---

## When in Doubt

1. **Check module-specific AGENTS.md** - More specific guidance
2. **Look at existing code** - Follow established patterns
3. **Run tests** - Ensure nothing breaks
4. **Ask for clarification** - Better to ask than assume

---

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT

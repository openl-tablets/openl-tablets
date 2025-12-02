# DEV Module - Claude Code Conventions

**Module**: OpenL Tablets DEV (Core Rules Engine)
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-12-02

This file provides specific guidance for the DEV module, which contains the core OpenL rules engine. For general project conventions, see the root `/AGENTS.md`.

---

## Module Overview

The DEV module is the heart of OpenL Tablets. It contains:

- **Core type system** (`IOpenClass`, `IOpenMethod`, `IOpenField`)
- **Parser and grammar** (JavaCC-based, BExGrammar)
- **Binding system** (type resolution, method binding)
- **Bytecode generation** (ASM-based rule compilation)
- **Runtime environment** (rule execution VM)
- **Project model** (rules project structure and loading)
- **Test infrastructure** (functional test framework)

**Critical Principle**: This is the foundation of the entire system. Changes here ripple through all dependent modules (STUDIO, WSFrontend, etc.).

---

## Module Structure

### Submodules

```
DEV/
├── org.openl.commons/               # Common utilities
│   ├── src/org/openl/
│   │   ├── util/                    # Utility classes
│   │   ├── message/                 # Message handling
│   │   └── meta/                    # Metadata utilities
│   └── test/                        # Common test utilities
│
├── org.openl.rules/                 # Main engine (largest, most critical)
│   ├── src/org/openl/
│   │   ├── types/                   # Core type system (IOpenClass, etc.)
│   │   ├── binding/                 # Type resolution, method binding
│   │   ├── rules/                   # Rule implementations, table types
│   │   ├── parser/                  # Parser implementation
│   │   ├── runtime/                 # Bytecode generation, VM
│   │   ├── source/                  # Source code abstractions
│   │   └── grammar/                 # Grammar definitions
│   ├── grammar/bexgrammar.jj        # JavaCC grammar (CRITICAL)
│   └── test/                        # Extensive unit tests
│
├── org.openl.rules.constrainer/     # Constraint solving
│   ├── src/org/openl/rules/constrainer/
│   └── test/
│
├── org.openl.rules.gen/             # Code generation
│   ├── src/org/openl/rules/gen/
│   │   └── binding/                 # Code gen for bindings
│   └── test/
│
├── org.openl.rules.project/         # Project model
│   ├── src/org/openl/rules/project/
│   │   ├── model/                   # Project structure
│   │   ├── impl/                    # Implementation
│   │   └── resolving/               # Resource resolution
│   └── test/
│
├── org.openl.rules.test/            # Functional test framework
│   ├── src/org/openl/rules/
│   │   └── test/                    # Test table implementations
│   └── test/
│
├── org.openl.rules.util/            # Utilities for rules
│   ├── src/org/openl/rules/util/
│   └── test/
│
└── org.openl.spring/                # Spring integration
    ├── src/org/openl/rules/spring/
    └── test/
```

---

## Build & Development

### Building the DEV Module

```bash
# Full build with tests
cd DEV && mvn clean install

# Quick build (recommended for development)
cd DEV && mvn clean install -Dquick -DnoPerf -T1C

# Skip tests
cd DEV && mvn clean install -DskipTests

# Build specific submodule
cd DEV/org.openl.rules && mvn clean install -DskipTests
```

### Building Just Grammar

If you only modified the grammar:

```bash
cd DEV/org.openl.rules && mvn clean compile
```

JavaCC will compile `grammar/bexgrammar.jj` to generate:
- `target/generated-sources/org/openl/rules/parser/`

### Common Build Issues

**Problem**: Grammar compilation fails
- Check for JavaCC syntax errors in `grammar/bexgrammar.jj`
- Rebuild entire module: `mvn clean compile`

**Problem**: Type system tests fail after changes to `IOpenClass`
- Run full test suite: `cd DEV && mvn test`
- May indicate breaking change in public API contract

---

## Critical Systems

### 1. Core Type System

**Location**: `org.openl.rules/src/org/openl/types/`

**Key Classes**:
- `IOpenClass` - Type definition (not Java `Class`)
- `IOpenMethod` - Method definition (not Java `Method`)
- `IOpenField` - Field definition (not Java `Field`)
- `IOpenDataType` - Data type marker interface

**Why Critical**:
- Foundation of entire system
- Every rule, every binding, every runtime feature depends on these contracts
- Changes break compatibility across all modules

**Rules for Changes**:
- Never remove methods from public interfaces
- Never change method signatures
- Never change method semantics without major version bump
- Test exhaustively before committing
- Add new capabilities via new interfaces, not existing ones

**When Making Changes**:
1. Check all callers: `grep -r "IOpenClass" DEV/org.openl.rules/src/`
2. Run full test suite: `cd DEV && mvn test`
3. Run integration tests: `cd ITEST/itest.smoke && mvn verify`
4. Document breaking changes clearly

---

### 2. Parser & Grammar (JavaCC)

**Location**: `org.openl.rules/grammar/bexgrammar.jj`

**What It Does**:
- Defines OpenL business expression syntax
- Compiles to `org.openl.rules.parser.SimpleCharStream`, `Parser`, etc.
- Used for every rule, every cell expression, every binding

**Why Critical**:
- Syntax changes affect all rules everywhere
- Parser errors prevent rule loading
- Backward compatibility is essential

**Grammar Structure**:
- Token definitions (lexical analysis)
- Production rules (syntax analysis)
- Node builders (AST construction)

**When Modifying Grammar**:
1. Document new syntax in `Docs/business-language.md`
2. Add positive test cases: parsing should succeed
3. Add negative test cases: invalid syntax should fail gracefully
4. Test with real Excel files in `ITEST/itest.smoke`
5. Ensure backward compatibility: old rules still parse

**Common Tasks**:

```bash
# Test grammar changes
cd DEV/org.openl.rules
mvn clean compile              # Recompile grammar
mvn test -Dtest=ParserTest    # Run parser unit tests
cd ../../ITEST/itest.smoke && mvn verify  # Integration tests
```

**Warning Signs**:
- Parser hangs (infinite loop in production rule)
- Ambiguous grammar (shift/reduce conflicts in JavaCC output)
- Regression: old syntax no longer works

---

### 3. Binding System

**Location**: `org.openl.rules/src/org/openl/binding/`

**What It Does**:
- Type resolution (what type is this expression?)
- Method binding (which method to call?)
- Overload resolution (which overload matches?)
- Type checking (are arguments valid for this method?)
- Implicit type conversion (can we auto-convert types?)

**Key Classes**:
- `NodeBinder` - Base class for binding syntax nodes
- `ANodeBinder` - Abstract base for common binding logic
- `TypeBoundNode` - Result of binding (expression with type information)
- `*Binder` subclasses - Specific node types (CallBinder, BinaryBinder, etc.)

**Why Critical**:
- Complex overload resolution logic
- Performance-sensitive code path
- Errors here cause runtime failures

**Performance Considerations**:
- Caching: Use `TypeResolver` for type lookups
- Avoid reflection: Use OpenL types, not Java reflection
- Method lookup: Use indexed/hashable method names when possible

**When Making Changes**:
1. Understand the visitor pattern (AST traversal)
2. Test with complex overload scenarios
3. Verify performance: check for new O(n²) loops
4. Run `cd DEV && mvn test` for regression tests

---

### 4. Bytecode Generation (ASM)

**Location**: `org.openl.rules/src/org/openl/rules/runtime/`

**What It Does**:
- Generates JVM bytecode for compiled rules
- Creates method implementations for rule methods
- Optimizes method dispatch
- Handles type conversions at bytecode level

**ASM Library**:
- Version: 9.9
- Used for low-level bytecode manipulation
- Generated code is native JVM bytecode (fast)

**Why Critical**:
- Errors generate invalid bytecode
- Invalid bytecode causes runtime crashes or incorrect results
- Difficult to debug (compiled code, not source)

**Development Tips**:
- Use ASMifier tool to verify generated bytecode
- Test with various JVM versions (21, 25)
- Include edge cases: null handling, type conversions

**When Making Changes**:
1. Understand bytecode basics (stack operations, method calls)
2. Test generated code thoroughly
3. Use ASMifier to inspect generated bytecode: `java -classpath asm.jar org.objectweb.asm.util.ASMifier`
4. Run full test suite with various JVM versions

---

## Code Organization Conventions

### Package Structure

```
org.openl
├── types/              # Core type system (IOpenClass, etc.)
├── binding/            # Type resolution and binding
├── rules/
│   ├── dt/            # Decision tables
│   ├── table/         # Generic table types
│   ├── method/        # Method rules
│   ├── property/      # Property rules
│   ├── spreadsheet/   # Spreadsheet rules
│   ├── runtime/       # Bytecode generation
│   └── ...
├── source/            # Source abstractions
├── parser/            # Parser generated code
└── util/              # Utilities
```

### Class Naming Conventions

**Interfaces**:
- `IOpenClass` - Public API interfaces
- `ITableType` - Type marker interfaces
- `ISyntaxNode` - Visitor pattern targets

**Abstract Classes**:
- `ANodeBinder` - Base class for binders
- `ATableType` - Base class for table types
- `AOpenClass` - Base class for type implementations

**Implementations**:
- `StaticOpenClass` - Static type from Java class
- `DynamicOpenClass` - Dynamic type from data
- `ComponentOpenClass` - Composite type
- `CustomSpreadsheetType` - Custom spreadsheet implementation

### Method Naming

Standard Java conventions with specific patterns:

```java
// Type system methods
String getName();           // Get name
IOpenClass[] getSignature(); // Get method signature
Object newInstance(IRuntimeEnv env);  // Create instance

// Binding methods
IOpenClass bind(ICachedBinder binder, IBoundNode target);
IBoundNode bind(ISyntaxNode node, IBinder binder);

// Visitor pattern
void accept(ISyntaxNodeVisitor visitor);
```

---

## Testing Strategy for DEV Module

### Unit Test Organization

**Test Location**: `org.openl.rules/test/org/openl/rules/`

**Test Organization**:
```
test/
├── binding/         # Binding system tests
├── types/           # Type system tests
├── parser/          # Parser tests
├── table/           # Table type tests
├── dt/              # Decision table tests
├── method/          # Method rule tests
├── runtime/         # Runtime/bytecode tests
└── ...
```

### Test Naming Convention

```java
// Standard format: testMethodName_scenario_expectedResult
@Test
void testBind_validMethodCall_returnsTypedBoundNode() {
    // Arrange
    ISyntaxNode syntaxNode = parseExpression("method(1, 2)");

    // Act
    IBoundNode boundNode = binder.bind(syntaxNode);

    // Assert
    assertNotNull(boundNode);
    assertEquals(Integer.class, boundNode.getType());
}
```

### Running Tests

```bash
# All DEV tests
cd DEV && mvn test

# Specific test class
mvn test -Dtest=ParserTest

# Specific test method
mvn test -Dtest=ParserTest#testParse_validExpression_returnsAST

# With coverage
mvn test jacoco:report

# Debug mode (attach debugger on port 5005)
mvn -Dmaven.surefire.debug test
```

### Test Exclusions

Some tests are intentionally excluded (known broken):
- Check `DEV/org.openl.rules.constrainer/pom.xml` for exclusion patterns
- Document reason in module README or code comments

---

## Common Development Tasks

### Task: Add a New Table Type

**Steps**:
1. Extend `ATableType` or implement `ITableType`
2. Define table syntax (header, body format)
3. Create node binder in `org.openl.rules/src/org/openl/rules/lang/xls/binding/`
4. Create bound node class (extends `ATableBoundNode`)
5. Register in `XlsDefinitions` or table type registry
6. Write unit tests for parsing and binding
7. Write integration tests in `ITEST/itest.smoke`
8. Document syntax in `Docs/business-language.md`

**Example Locations**:
- Decision Table: `rules/dt/DecisionTableType.java`
- Method Rule: `rules/method/MethodType.java`
- Spreadsheet: `rules/spreadsheet/SpreadsheetType.java`

### Task: Extend Type System

**Steps**:
1. Consider if `IOpenClass` interface needs changes
2. If adding new capability: create new interface extending `IOpenClass`
3. Implement in appropriate `*OpenClass` subclass
4. Update type resolution in binding system if needed
5. Write comprehensive tests for new type
6. Verify all existing type tests still pass
7. Document in architecture guide

### Task: Optimize Binding Performance

**Steps**:
1. Profile with real rules using `org.openl.rules.profiler`
2. Identify bottleneck (type resolution, method lookup, etc.)
3. Consider caching (if result is deterministic)
4. Review AGENTS.md section on Binding System
5. Implement optimization with minimal code change
6. Verify with performance tests
7. Run full test suite to ensure no regressions

### Task: Fix a Parser Issue

**Steps**:
1. Create test case that reproduces the issue
2. Check if it's a grammar issue or binding issue
3. If grammar: modify `bexgrammar.jj`
4. If binding: fix appropriate binder class
5. Add test to prevent regression
6. Run `ITEST/itest.smoke` integration tests
7. Verify backward compatibility with existing rules

---

## Memory Management & Performance

### Memory Leak Detection

The DEV module runs tests with strict memory constraints to catch leaks:

```bash
# Default: strict memory limits detect leaks
mvn test

# Development: relaxed memory limits for faster iteration
mvn test -DnoPerf
```

**Known Memory Issues** (from AGENTS.md):
- `JavaOpenClassCache` - Needs review for potential leaks
- `RuleRowHelper` - String interning may cause memory issues

### Optimization Opportunities

**Type Resolution Caching**:
- `TypeResolver` has basic caching
- Consider adding more aggressive caching for repeated lookups

**Method Binding Caching**:
- Method lookup results can be cached
- Especially for generated proxies (stable at runtime)

**Bytecode Generation**:
- Already optimized for native JVM execution
- Focus on generation time, not execution time

---

## Known Issues & Technical Debt

From root AGENTS.md, specific to DEV:

1. **Memory Leaks**:
   - `JavaOpenClassCache` (needs review)
   - `RuleRowHelper` string interning (needs review)

2. **Architecture Issues**:
   - Method calling in constructor: `ComponentOpenClass`
   - Public fields in `BinaryNode`, `UnaryNode` (should be private)
   - Static method access design in `TypeBoundNode`

3. **Feature Gaps**:
   - Security blocking for system class access: `TypeResolver`
   - Generic type support missing: `CastToWiderType`
   - Array dimension initialization: `NewArrayNodeBinder`

When fixing bugs in these areas, consider addressing technical debt at the same time.

---

## Integration with Other Modules

### DEV → STUDIO

STUDIO depends on DEV for:
- Rule compilation (`CompiledOpenClass`, `OpenL`)
- Type system (`IOpenClass`, `IOpenMethod`)
- Parser (`Parser`, `BExGrammar`)
- Project model (`RulesProject`)

**Implication**: Breaking changes in DEV require STUDIO updates.

### DEV → WSFrontend

WSFrontend depends on DEV for:
- Rule execution
- Type conversion
- Method invocation
- Runtime environment

**Implication**: Runtime bugs in DEV affect all deployed services.

### Testing with Dependents

After significant DEV changes:

```bash
# Build STUDIO to check compatibility
cd STUDIO && mvn clean install -DskipTests

# Run integration tests
cd ITEST/itest.smoke && mvn verify
```

---

## Code Review Checklist for DEV Changes

Before committing DEV module changes:

- [ ] **Type System Changes**: Verify all implementations updated
- [ ] **Grammar Changes**: Tested with integration tests
- [ ] **Binding Changes**: No performance regressions
- [ ] **Bytecode Generation**: Verified with ASMifier
- [ ] **Tests**: Full test suite passes (`mvn test`)
- [ ] **Integration**: STUDIO and WSFrontend still build
- [ ] **Documentation**: Updated if changing public APIs
- [ ] **Backward Compatibility**: Old rules still compile
- [ ] **Memory**: No new memory leaks (use `-DnoPerf` during development)

---

## For More Information

- **Root Project Conventions**: `/AGENTS.md`
- **Type System Details**: See `org.openl.rules/src/org/openl/types/`
- **Grammar Reference**: `org.openl.rules/grammar/bexgrammar.jj` comments
- **Architecture**: `/Docs/ARCHITECTURE.md`
- **Parser Details**: `/Docs/developer-guides/`

---

**Last Updated**: 2025-12-02
**Version**: 6.0.0-SNAPSHOT

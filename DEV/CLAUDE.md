# DEV Module - Core Engine Conventions

**Module**: DEV (Core Rules Engine)
**Location**: `/home/user/openl-tablets/DEV/`
**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-05

---

## Module Purpose

The DEV module contains the **core OpenL Tablets rules engine**, responsible for:
- Parsing Excel spreadsheets into executable code
- Type system and method resolution
- Bytecode generation and execution
- Project management and Spring integration

**Critical**: This is the foundation of OpenL Tablets. Changes here affect the entire system.

---

## Submodules

| Submodule | Purpose | Sensitivity |
|-----------|---------|-------------|
| `org.openl.commons` | Foundation utilities | 🟢 Low risk |
| `org.openl.rules` | **Core engine** | 🔴 **CRITICAL** |
| `org.openl.rules.annotations` | Custom annotations | 🟢 Low risk |
| `org.openl.rules.util` | Built-in functions | 🟡 Medium risk |
| `org.openl.rules.gen` | Code generation (build-time) | 🟡 Medium risk |
| `org.openl.rules.constrainer` | Constraint solver | 🟢 Low risk (rarely used) |
| `org.openl.rules.project` | Project management | 🟡 Medium risk |
| `org.openl.spring` | Spring integration | 🟡 Medium risk |
| `org.openl.rules.test` | Testing framework | 🟢 Low risk |

---

## Core Engine (`org.openl.rules`) - CRITICAL AREA

### ⚠️ Warning: High-Impact Zone

The `org.openl.rules` module is the heart of OpenL. Changes here must be:
- **Exhaustively tested**
- **Backward compatible**
- **Performance-conscious**
- **Reviewed carefully**

### Architecture Layers

```
┌─────────────────────────────────────┐
│  Runtime & Execution                │  🔴 CRITICAL
│  (SimpleRulesVM, Proxies)           │
├─────────────────────────────────────┤
│  Code Generation                    │  🔴 CRITICAL
│  (ASM Bytecode)                     │
├─────────────────────────────────────┤
│  Binding & Type Resolution          │  🔴 CRITICAL
│  (IBindingContext, Node Binders)    │
├─────────────────────────────────────┤
│  Parsing                            │  🔴 CRITICAL
│  (BExGrammar, Parser)               │
├─────────────────────────────────────┤
│  Type System                        │  🔴 CRITICAL
│  (IOpenClass, IOpenMethod)          │
└─────────────────────────────────────┘
```

### Do NOT Touch Without Expert Review

1. **Type System Contracts** (`/src/org/openl/types/`)
   - `IOpenClass`, `IOpenMethod`, `IOpenField`
   - Changes break every feature

2. **Parser Grammar** (`/grammar/bexgrammar.jj`)
   - Requires JavaCC expertise
   - Affects all Excel rules
   - Extensive regression testing required

3. **Bytecode Generation** (`/src/org/openl/rules/runtime/`)
   - Requires ASM expertise
   - JVM bytecode knowledge required
   - Errors cause runtime crashes

4. **Binding System** (`/src/org/openl/binding/`)
   - Complex type resolution
   - Performance-critical
   - Affects method dispatch

### Safe to Modify (with Tests)

1. **Utilities** (`/src/org/openl/util/`)
   - Helper functions
   - Formatters, collections

2. **Data Structures** (new data types)
   - Add new table types (with proper extension)
   - New node binders (following patterns)

3. **Validation** (`/src/org/openl/validation/`)
   - Add validation rules
   - Improve error messages

---

## Type System Conventions

### Naming Conventions

**Interfaces**: Prefix with `I`
```java
IOpenClass
IOpenMethod
IOpenField
```

**Abstract Classes**: Prefix with `A` or suffix with `Base`
```java
AOpenClass
BindingContextBase
```

**Implementations**: Descriptive names
```java
StaticOpenClass      // Wraps Java classes
DynamicOpenClass     // Runtime-created types
ComponentOpenClass   // Complex types
```

### Type System Rules

1. **Never break interface contracts**
   - `IOpenClass` is used everywhere
   - Maintain backward compatibility
   - Add new methods to subinterfaces if needed

2. **Use OpenL types internally**
   - Don't leak Java `Class`, `Method`, `Field`
   - Convert at system boundaries only

3. **Handle null carefully**
   - Use `NullOpenClass` for null types
   - Check for null in type operations

### Example: Adding a New IOpenClass Implementation

```java
/**
 * Custom open class for specialized types.
 */
public class MyOpenClass extends AOpenClass {

    @Override
    public String getName() {
        return "MyType";
    }

    @Override
    public IOpenMethod getMethod(String name, IOpenClass[] params) {
        // Implementation
    }

    // ... other required methods
}
```

**Checklist**:
- ✅ Extends `AOpenClass` or implements `IOpenClass`
- ✅ Overrides all abstract methods
- ✅ Includes JavaDoc
- ✅ Has unit tests
- ✅ Handles edge cases (null, empty arrays)

---

## Parser & Grammar Conventions

### Grammar File

**Location**: `/home/user/openl-tablets/DEV/org.openl.rules/grammar/bexgrammar.jj`
**Format**: JavaCC grammar

### Modifying Grammar

**⚠️ EXTREME CAUTION REQUIRED**

**Before modifying**:
1. Understand JavaCC syntax
2. Read existing grammar thoroughly
3. Identify impact on existing rules
4. Plan backward compatibility

**After modifying**:
1. Regenerate parser: `mvn generate-sources`
2. Test with ALL existing test files
3. Create regression test suite
4. Document syntax changes

### Grammar Conventions

1. **Maintain backward compatibility**
   - Never break existing syntax
   - Add new productions, don't modify existing

2. **Follow naming patterns**
   - Productions: `PascalCase`
   - Tokens: `UPPER_CASE`

3. **Document additions**
   ```javacc
   /*
    * NEW SYNTAX: Support for range expressions
    * Example: 1..10
    */
   void RangeExpression() : {}
   {
       // Production rule
   }
   ```

---

## Binding System Conventions

### Node Binders

**Location**: `/src/org/openl/binding/` and `/src/org/openl/rules/binding/`

### Creating a New Node Binder

**Pattern**:
```java
/**
 * Binds MyNewSyntax nodes to executable code.
 */
public class MyNewSyntaxNodeBinder implements INodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
        throws Exception {

        // 1. Validate syntax node
        if (node.getType() != NodeType.MY_NEW_SYNTAX) {
            throw new SyntaxNodeException("Unexpected node type", node);
        }

        // 2. Bind child nodes
        ISyntaxNode[] children = node.getChildren();
        IBoundNode[] boundChildren = new IBoundNode[children.length];
        for (int i = 0; i < children.length; i++) {
            boundChildren[i] = bindingContext.bind(children[i]);
        }

        // 3. Resolve types
        IOpenClass resultType = determineResultType(boundChildren);

        // 4. Create and return bound node
        return new MyNewSyntaxBoundNode(node, boundChildren, resultType);
    }

    private IOpenClass determineResultType(IBoundNode[] children) {
        // Type resolution logic
    }
}
```

### Binding Conventions

1. **Register binder**
   - Add to `NodeBinderRegistry` or configuration

2. **Error handling**
   ```java
   // Collect errors, don't throw immediately
   bindingContext.addError(new SyntaxNodeException("Error message", node));
   ```

3. **Type resolution**
   - Use `MethodUtil` for method resolution
   - Use `CastFactory` for type casting
   - Handle type conversions properly

4. **Performance**
   - Cache resolved methods/types when possible
   - Avoid repeated lookups

---

## Bytecode Generation Conventions

### ASM Usage

**Location**: `/src/org/openl/rules/runtime/`

### ⚠️ ASM Is Complex - Expert Knowledge Required

**Before modifying bytecode generation**:
1. Understand JVM bytecode specification
2. Know ASM library well
3. Use `ASMifier` to verify bytecode
4. Test with multiple JVM versions

### Bytecode Generation Patterns

```java
// Example: Generating a simple method
ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internalName, null,
    superClass, interfaces);

MethodVisitor mv = cw.visitMethod(
    Opcodes.ACC_PUBLIC,
    methodName,
    methodDescriptor,
    null,
    null
);

mv.visitCode();
// ... emit bytecode instructions
mv.visitMaxs(0, 0);  // Auto-computed with COMPUTE_FRAMES
mv.visitEnd();

byte[] bytecode = cw.toByteArray();
```

### Bytecode Conventions

1. **Use COMPUTE_FRAMES**
   - Simplifies stack frame management
   - Required for Java 21+

2. **Verify bytecode**
   ```java
   // In tests
   CheckClassAdapter.verify(new ClassReader(bytecode), false, new PrintWriter(System.err));
   ```

3. **Test thoroughly**
   - Unit tests with various inputs
   - Integration tests with real rules
   - Performance tests

4. **Debug with ASMifier**
   ```bash
   java -classpath asm.jar org.objectweb.asm.util.ASMifier MyClass.class
   ```

---

## Table Type Conventions

### Decision Tables

**Location**: `/src/org/openl/rules/dt/`

**Key Classes**:
- `IDecisionTable` - Interface
- `DecisionTable` - Implementation
- `DecisionTableDataType` - Type representation

### Adding Decision Table Features

**Pattern**:
1. Extend `DecisionTable` or create new `IDecisionTable` implementation
2. Update `DecisionTableBoundNode` if needed
3. Modify algorithm compiler if matching logic changes
4. Add validation rules
5. Update documentation

### Data Tables

**Location**: `/src/org/openl/rules/data/`

**Conventions**:
- Use `ITableModel` for grid access
- Use `DataOpenField` for field access
- Handle type conversions properly

### Spreadsheet Tables

**Location**: `/src/org/openl/rules/calc/`

**Conventions**:
- Cell formulas use binding system
- Cross-cell references via `FieldChain`
- Lazy evaluation where possible

---

## Project Management Conventions

### Project Descriptor

**Location**: `/src/org/openl/rules/project/model/ProjectDescriptor.java`

**Conventions**:
1. **JAXB annotations required**
   - For XML serialization
   - Maintain backward compatibility

2. **Immutability preferred**
   - Builders for complex construction
   - Defensive copies

3. **Validation**
   - Use `ProjectDescriptorValidator`
   - Clear error messages

### Instantiation Strategies

**Location**: `/src/org/openl/rules/project/instantiation/`

**Creating Custom Strategy**:
```java
public class MyInstantiationStrategy implements RulesInstantiationStrategy {

    @Override
    public Object instantiate(CompiledOpenClass compiledClass) {
        // Custom instantiation logic
    }

    @Override
    public Class<?> getServiceClass() {
        // Return service interface
    }
}
```

---

## Testing Conventions

### Unit Tests

**Structure**:
```
test/
├── org/openl/types/           # Type system tests
├── org/openl/binding/         # Binding tests
├── org/openl/rules/dt/        # Decision table tests
└── ...
```

**Naming**:
```java
class IOpenClassTest {
    @Test
    void testGetMethod_validName_returnsMethod() { }

    @Test
    void testGetMethod_invalidName_returnsNull() { }

    @Test
    void testNewInstance_abstractClass_throwsException() { }
}
```

### Integration Tests

**Location**: Use `/ITEST/` for integration tests, not DEV module

### Test Resources

**Location**: `/test-resources/`

**Structure**:
```
test-resources/
├── functionality/
│   ├── algorithms/
│   ├── datatypes/
│   └── ...
└── syntax/
    ├── valid/
    └── invalid/
```

### Testing Rules

1. **Test public APIs thoroughly**
2. **Include edge cases**
   - Null inputs
   - Empty arrays
   - Invalid types
3. **Test error conditions**
4. **Use meaningful assertions**
   ```java
   assertEquals(expected, actual, "Should calculate correct premium");
   ```

---

## Performance Guidelines

### Compilation Performance

**Hotspots**:
- Excel parsing (POI)
- Type resolution
- Method binding
- Bytecode generation

**Optimization**:
1. **Cache aggressively**
   ```java
   // Cache compiled classes
   private final Map<String, CompiledOpenClass> cache = new ConcurrentHashMap<>();
   ```

2. **Lazy loading**
   ```java
   // Don't load modules until needed
   if (module.isRequired()) {
       loadModule(module);
   }
   ```

3. **Indexing**
   ```java
   // Use rule indexes for fast lookup
   IRuleIndex index = buildIndex(rules);
   ```

### Runtime Performance

**Critical Paths**:
- Method dispatch
- Type checking
- Context lookups

**Avoid**:
- Reflection in hot paths
- Synchronization where not needed
- Unnecessary object creation

**Profile**:
```bash
# Use JMH for microbenchmarks
mvn test -Pbenchmark

# Use profiler module
// Code
```

---

## Deprecation Policy

### Marking Code as Deprecated

```java
/**
 * Old method - use {@link #newMethod()} instead.
 *
 * @deprecated Since 6.0.0, use {@link #newMethod()} which provides better performance
 */
@Deprecated(since = "6.0.0", forRemoval = true)
public void oldMethod() {
    // Keep implementation for backward compatibility
}
```

### Deprecation Process

1. **Mark with `@Deprecated`**
2. **Add JavaDoc explaining alternative**
3. **Update documentation**
4. **Announce in release notes**
5. **Remove in next major version** (if `forRemoval = true`)

### Currently Deprecated

1. **`OpenL.getInstance()`**
   - Use explicit configuration instead
   - Planned removal: 7.0.0

2. **Operator methods in `Operators` class**
   - Use annotation-based operators
   - Planned removal: 7.0.0

3. **Legacy source code modules**
   - Use simpler abstractions
   - Planned removal: 6.1.0

---

## Known Issues & TODOs

### Critical TODOs

1. **Method calling in constructor** - `ComponentOpenClass:52`
   - Issue: Overrideable method called in constructor
   - Risk: NPE or incorrect behavior
   - Fix: Refactor to initialization method

2. **Cache memory leak risk** - `JavaOpenClassCache:156`
   - Issue: Cache may grow unbounded
   - Risk: OutOfMemoryError
   - Fix: Add cache eviction policy

3. **String interning review** - `RuleRowHelper:234`
   - Issue: Excessive string interning
   - Risk: PermGen/Metaspace issues
   - Fix: Review interning strategy

### Feature Gaps

1. **Generic type support** - `CastToWiderType:78`
   - Missing: Full Java generics support
   - Workaround: Use raw types
   - Plan: Add in 6.1.0

2. **Array dimension initialization** - `NewArrayNodeBinder:45`
   - Missing: Multi-dimensional array initialization
   - Workaround: Use single-dimensional
   - Plan: Extend grammar

### Test Issues

1. **Constrainer tests excluded**
   - Tests: `TestExecutionControl*`
   - Status: Broken, excluded from build
   - Plan: Fix or remove constrainer module

---

## Code Review Checklist

### Before Submitting PR

- ✅ All tests pass
- ✅ New code has unit tests
- ✅ JavaDoc for public APIs
- ✅ No new compiler warnings
- ✅ Performance impact considered
- ✅ Backward compatibility maintained
- ✅ Documentation updated if needed

### For Reviewers

- ✅ Type system contracts preserved
- ✅ Binding logic correct
- ✅ Error handling proper
- ✅ Performance acceptable
- ✅ Test coverage adequate
- ✅ Code follows conventions

---

## Emergency Contacts & Escalation

### Critical Issues

If you discover:
- **Security vulnerability** → Escalate immediately
- **Data corruption risk** → Stop, escalate
- **Backward compatibility break** → Escalate before merging

### Expert Review Required For

- Grammar changes
- Type system changes
- Bytecode generation changes
- Binding logic changes

---

## Resources

### Module Documentation

- [DEV Module Overview](/docs/analysis/dev-module-overview.md) - Comprehensive module analysis
- [Technology Stack](/docs/architecture/technology-stack.md) - Technology details
- [Codebase Tour](/docs/onboarding/codebase-tour.md) - Navigate the code

### Key Files

| File | Purpose |
|------|---------|
| `/DEV/pom.xml` | Module build configuration |
| `/DEV/org.openl.rules/grammar/bexgrammar.jj` | Parser grammar |
| `/DEV/org.openl.rules/src/org/openl/types/IOpenClass.java` | Type system contract |

### External Resources

- **ASM Guide**: https://asm.ow2.io/asm4-guide.pdf
- **JavaCC Tutorial**: https://javacc.github.io/javacc/
- **JVM Specification**: https://docs.oracle.com/javase/specs/jvms/se21/html/

---

## Summary

**DEV module is CRITICAL**. When in doubt:
1. ✅ **Test thoroughly**
2. ✅ **Review carefully**
3. ✅ **Maintain backward compatibility**
4. ✅ **Document changes**
5. ✅ **Ask for expert review**

---

**Last Updated**: 2025-11-05
**Module Version**: 6.0.0-SNAPSHOT

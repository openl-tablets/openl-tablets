# Claude Code Workflows for OpenL Tablets

**Last Updated**: 2025-11-05
**Purpose**: Guide for using Claude Code effectively with OpenL Tablets

---

## Introduction

This document provides guidance for using Claude Code when working with the OpenL Tablets codebase. It covers common workflows, best practices, and patterns specific to this repository.

---

## Quick Start with Claude Code

### First Time Setup

1. **Read these files first**:
   ```
   /CLAUDE.md              # Repository conventions
   /DEV/CLAUDE.md          # Core engine conventions
   /README.md              # Build instructions
   /docs/onboarding/codebase-tour.md  # Repository tour
   ```

2. **Understand the structure**:
   - DEV = Core engine (critical, handle with care)
   - STUDIO = Web IDE and repositories
   - WSFrontend = Rule services
   - Util = Tools and utilities

3. **Check documentation**:
   ```
   /docs/architecture/      # System architecture
   /docs/analysis/          # Module deep dives
   /docs/onboarding/        # Getting started
   ```

---

## Common Workflows

### Workflow 1: Understanding Code

**Task**: "How does decision table compilation work?"

**Approach**:
```
1. Start with high-level docs:
   - Read /docs/analysis/dev-module-overview.md
   - Section: "Decision Tables"

2. Locate relevant code:
   - Use Glob: "DEV/org.openl.rules/src/org/openl/rules/dt/**/*.java"
   - Find IDecisionTable.java

3. Read key classes:
   - IDecisionTable (interface)
   - DecisionTable (implementation)
   - DecisionTableBoundNode (compilation)
   - DecisionTableAlgorithm (execution)

4. Trace compilation flow:
   - Parser → Binder → Algorithm Compiler → Executor
```

**Claude Code Commands**:
```
"Read the decision table compilation flow"
"Explain how DecisionTableBoundNode works"
"Show me the algorithm compiler implementations"
```

### Workflow 2: Adding a New Feature

**Task**: "Add a new validation rule for decision tables"

**Approach**:
```
1. Understand existing validation:
   - Read /DEV/CLAUDE.md section on validation
   - Locate org.openl.rules.dt.validator package
   - Study DecisionTableValidator.java

2. Plan the change:
   - Identify where validation fits
   - Check if extension point exists
   - Design validation logic

3. Implement:
   - Create new validator class
   - Register validator
   - Add tests

4. Test:
   - Unit tests (JUnit 5)
   - Integration tests
   - Test with sample rules

5. Document:
   - Update JavaDoc
   - Add example in docs if significant
```

**Claude Code Prompts**:
```
"Show me existing decision table validators"
"Create a new validator for [specific rule]"
"Generate unit tests for my validator"
"Update documentation for the new validation"
```

### Workflow 3: Fixing a Bug

**Task**: "Fix compilation error in datatype tables"

**Approach**:
```
1. Reproduce the bug:
   - Create minimal test case
   - Document steps to reproduce

2. Locate the issue:
   - Check stack trace for file/line
   - Read related code in /docs/analysis/
   - Use Grep to find related code

3. Identify root cause:
   - Trace execution path
   - Check for recent changes (git log)
   - Review related TODOs/FIXMEs

4. Fix:
   - Make minimal change
   - Ensure fix doesn't break other features
   - Add regression test

5. Verify:
   - Run existing tests
   - Run new regression test
   - Test with affected rules
```

**Claude Code Prompts**:
```
"Analyze this stack trace: [paste trace]"
"Find all usages of DatatypeTableBoundNode"
"Create a regression test for this bug"
"Review my fix for potential issues"
```

### Workflow 4: Refactoring

**Task**: "Refactor duplicated code in rule binding"

**Approach**:
```
1. Identify duplication:
   - Use Grep to find similar patterns
   - Analyze code structure

2. Plan refactoring:
   - Extract common functionality
   - Design abstraction
   - Check impact (who uses this code?)

3. Implement:
   - Create base class/interface
   - Move common code
   - Update callers

4. Test thoroughly:
   - All existing tests must pass
   - Add tests for new abstraction
   - Check performance impact

5. Document:
   - Update JavaDoc
   - Add to CLAUDE.md if pattern is reusable
```

**Claude Code Prompts**:
```
"Find duplicated code in org.openl.binding package"
"Suggest refactoring for [code block]"
"Extract common functionality into base class"
"Ensure all tests pass after refactoring"
```

### Workflow 5: Performance Optimization

**Task**: "Optimize rule execution performance"

**Approach**:
```
1. Profile:
   - Use org.openl.rules.profiler
   - Identify bottlenecks
   - Measure baseline performance

2. Analyze:
   - Read profiling data
   - Check algorithm complexity
   - Look for unnecessary operations

3. Optimize:
   - Add caching where appropriate
   - Optimize algorithms
   - Reduce object creation

4. Verify:
   - Re-profile
   - Compare before/after
   - Ensure correctness maintained

5. Document:
   - Add comments explaining optimization
   - Note performance characteristics
```

**Claude Code Prompts**:
```
"Analyze profiling data: [paste data]"
"Suggest optimizations for [method]"
"Add caching to [class]"
"Verify performance improvement"
```

---

## Module-Specific Workflows

### Working with DEV Module (Core Engine)

**⚠️ CRITICAL**: Changes here affect entire system

**Workflow**:
```
1. Read /DEV/CLAUDE.md thoroughly
2. Understand impact (check dependency graph)
3. Test exhaustively
4. Get expert review before merging
```

**Common Tasks**:

**Add new table type**:
```
1. Define syntax in Excel
2. Create XlsDefinition entry
3. Implement NodeBinder
4. Implement BoundNode
5. Add to type system
6. Write comprehensive tests
```

**Modify parser grammar**:
```
1. ⚠️ EXTREME CAUTION
2. Read grammar file thoroughly
3. Plan backward compatibility
4. Modify bexgrammar.jj
5. Regenerate parser (mvn generate-sources)
6. Test ALL existing rules
7. Document syntax change
```

### Working with STUDIO Module

**Less critical than DEV, but still important**

**Common Tasks**:

**Add REST endpoint**:
```
1. Create @RestController
2. Define DTO classes
3. Implement logic
4. Add OpenAPI annotations
5. Write integration tests
6. Test with Postman
```

**Add UI feature** (React):
```
1. cd STUDIO/studio-ui
2. Create component in src/components/
3. Add routing if needed
4. Connect to backend API
5. Write unit tests (Jest)
6. Test in browser
```

### Working with WSFrontend Module

**Service deployment and execution**

**Common Tasks**:

**Add deployment filter**:
```
1. Understand existing filters
2. Implement DeploymentFilter interface
3. Register in configuration
4. Test with sample deployments
```

**Add Kafka integration**:
```
1. Configure Kafka properties
2. Implement message handler
3. Add serialization support
4. Test with Kafka cluster
```

---

## Best Practices

### 1. Always Read Docs First

**Before making changes**:
```
✅ Read /CLAUDE.md
✅ Read module-specific CLAUDE.md
✅ Check /docs/analysis/ for module overview
✅ Review existing similar code
```

**Don't**:
```
❌ Jump straight to coding
❌ Ignore existing patterns
❌ Skip documentation
```

### 2. Test Thoroughly

**Test levels**:
```
1. Unit tests (JUnit 5)
   - Test individual methods
   - Mock dependencies
   - Edge cases

2. Integration tests (/ITEST/)
   - Test module interactions
   - Use TestContainers
   - Real databases/services

3. Manual testing
   - Test with OpenL Studio UI
   - Test with RuleService
   - Test with sample rules
```

**Coverage requirements**:
```
- Core engine (DEV): >80%
- Other modules: >60%
- New code: 100% of public APIs
```

### 3. Follow Conventions

**Code style**:
```java
// ✅ Good
public interface IOpenClass {
    String getName();
    IOpenMethod getMethod(String name, IOpenClass[] params);
}

// ❌ Bad (inconsistent naming)
public interface OpenClass {
    string name();
    OpenMethod findMethod(string methodName, OpenClass[] parameters);
}
```

**Commit messages**:
```
✅ Good:
feat: Add support for nested datatype tables

Implement parsing and binding for nested datatype definitions.
This allows creating hierarchical data structures in Excel.

Closes #1234

❌ Bad:
Update code
```

### 4. Handle Legacy Code Carefully

**JSF/RichFaces** (being replaced):
```
✅ Bug fixes only
✅ Document "legacy" status
✅ Don't add features

❌ Don't enhance
❌ Don't refactor extensively
```

**Deprecated APIs**:
```
✅ Migrate to replacement
✅ Add deprecation notice
✅ Document alternative

❌ Don't use deprecated APIs in new code
```

### 5. Performance Awareness

**Hot paths** (frequently executed):
```
- Rule execution (SimpleRulesVM)
- Method dispatch
- Type resolution
- Excel parsing

✅ Optimize carefully
✅ Profile before/after
✅ Measure, don't guess
```

**Cold paths** (rarely executed):
```
- Compilation (one-time)
- Project loading
- Configuration reading

✅ Optimize only if problematic
```

---

## Claude Code Commands Reference

### Exploration

```
"Show me all decision table implementations"
"Find usages of IOpenClass"
"List all REST controllers in OpenL Studio"
"Show me the repository implementations"
```

### Understanding

```
"Explain how [class/method] works"
"What does this code do: [paste code]"
"Trace the execution path for [feature]"
"Show me the dependency graph for [module]"
```

### Implementation

```
"Create a new [feature] following existing patterns"
"Implement [interface] for [use case]"
"Add validation for [rule]"
"Generate tests for [class]"
```

### Debugging

```
"Analyze this error: [paste error]"
"Find the root cause of [issue]"
"Why is [feature] not working?"
"Check for potential issues in [code]"
```

### Testing

```
"Generate unit tests for [class]"
"Create integration test for [feature]"
"Add test cases for edge cases"
"Review test coverage"
```

### Documentation

```
"Document [class/method]"
"Update CLAUDE.md with [pattern]"
"Add example for [feature]"
"Generate API documentation"
```

---

## Working with Git

### Branch Strategy

**Feature branches**:
```bash
git checkout -b feature/add-new-validation
# Make changes
git commit -m "feat: Add validation for [feature]"
git push origin feature/add-new-validation
```

**Bug fix branches**:
```bash
git checkout -b fix/compilation-error
# Fix bug
git commit -m "fix: Resolve compilation error in datatypes"
git push origin fix/compilation-error
```

**Claude branches** (for Claude Code):
```bash
# Already created as: claude/document-repository-structure-<session-id>
git push -u origin claude/document-repository-structure-<session-id>
```

### Commit Guidelines

**Commit message format**:
```
<type>: <subject>

<body>

<footer>
```

**Types**:
```
feat     - New feature
fix      - Bug fix
refactor - Code refactoring
test     - Add/update tests
docs     - Documentation
build    - Build system changes
ci       - CI/CD changes
```

---

## Troubleshooting with Claude Code

### Build Fails

**Prompt**: "Help me debug this build error: [paste error]"

**Claude will**:
1. Analyze error message
2. Check related code
3. Suggest fixes
4. Verify solution

### Tests Fail

**Prompt**: "Why is this test failing: [paste test]"

**Claude will**:
1. Analyze test code
2. Check expectations vs actual
3. Review related changes
4. Suggest fixes

### Performance Issues

**Prompt**: "This code is slow, optimize it: [paste code]"

**Claude will**:
1. Analyze algorithm
2. Identify bottlenecks
3. Suggest optimizations
4. Verify correctness

---

## Advanced Workflows

### Multi-Module Changes

**Task**: Change affects DEV + STUDIO + WSFrontend

**Workflow**:
```
1. Plan changes across modules
2. Update DEV first (foundation)
3. Update dependent modules
4. Test integration
5. Commit atomically or in sequence
```

**Claude Code Approach**:
```
"Plan multi-module change for [feature]"
"Update DEV module for [change]"
"Update STUDIO to use new DEV API"
"Update WSFrontend accordingly"
"Test integration across modules"
```

### API Design

**Task**: Design new public API

**Workflow**:
```
1. Review existing API patterns
2. Design interface
3. Consider backward compatibility
4. Document thoroughly
5. Get review
6. Implement
```

**Claude Code Approach**:
```
"Review existing API patterns in [module]"
"Design API for [feature] following patterns"
"Check backward compatibility"
"Generate JavaDoc for new API"
```

---

## Tips for Effective Claude Code Usage

### 1. Be Specific

```
✅ Good: "Explain how DecisionTableBoundNode binds decision table syntax to executable code"
❌ Bad: "How do decision tables work?"
```

### 2. Provide Context

```
✅ Good: "I'm adding a new table type for workflow rules. Show me how to implement NodeBinder based on DecisionTableNodeBinder pattern."
❌ Bad: "How do I add a new table type?"
```

### 3. Iterative Approach

```
1. "Show me overview of [feature]"
2. "Explain [specific part] in detail"
3. "Help me implement [component]"
4. "Review my implementation"
5. "Generate tests"
```

### 4. Use Documentation

```
✅ "Based on /docs/analysis/dev-module-overview.md, help me understand [topic]"
✅ "Following patterns in /DEV/CLAUDE.md, implement [feature]"
```

### 5. Request Reviews

```
"Review this code for:
 - Correctness
 - Performance
 - Security issues
 - Convention compliance"
```

---

## Conclusion

Claude Code is a powerful tool for working with OpenL Tablets. By following these workflows and best practices, you can:

- Understand complex code quickly
- Implement features correctly
- Maintain code quality
- Follow project conventions
- Work efficiently

**Remember**:
- Always read documentation first
- Test thoroughly
- Follow conventions
- Ask for clarification when needed
- Review changes carefully

---

## See Also

- [CLAUDE.md](/CLAUDE.md) - Repository conventions
- [DEV/CLAUDE.md](/DEV/CLAUDE.md) - Core engine conventions
- [Development Setup](/docs/onboarding/development-setup.md) - Setup guide
- [Codebase Tour](/docs/onboarding/codebase-tour.md) - Repository tour

---

**Last Updated**: 2025-11-05
**Feedback**: Improve this document based on your experience

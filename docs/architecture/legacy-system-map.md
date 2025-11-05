# OpenL Tablets Legacy System Map

**Last Updated**: 2025-11-05
**Purpose**: Identify legacy components and migration status

---

## Overview

This document provides a complete map of legacy, modern, and transitional components in OpenL Tablets, helping developers understand which areas require special handling and which are safe to modernize.

---

## Status Legend

| Status | Symbol | Meaning | Action |
|--------|--------|---------|--------|
| **Modern** | ✅ | Current best practices | Safe to modify |
| **Transitional** | ⚠️ | Being migrated | Careful modification |
| **Legacy** | 🔴 | Outdated, maintain only | Bug fixes only |
| **Deprecated** | ❌ | Scheduled for removal | Do not use |
| **Custom Fork** | 🔧 | Maintained by OpenL | Special handling |

---

## Component Status Map

### DEV Module (Core Engine)

| Component | Status | Notes | Action Plan |
|-----------|--------|-------|-------------|
| **Type System** (`IOpenClass`, `IOpenMethod`) | ✅ Modern | Core abstraction | Continue using |
| **Parser** (JavaCC BExGrammar) | ✅ Modern | Stable | Maintain |
| **Binder** | ✅ Modern | Core functionality | Continue using |
| **ASM Bytecode Generation** | ✅ Modern | Java 21 compatible | Continue using |
| **Decision Tables** | ✅ Modern | Production-ready | Continue using |
| **Spreadsheet Tables** | ✅ Modern | Production-ready | Continue using |
| **Data Tables** | ✅ Modern | Production-ready | Continue using |
| **Algorithm/TBasic** | ⚠️ Transitional | Limited usage | Evaluate need |
| **Column Match** | ⚠️ Transitional | Niche use case | Evaluate need |
| **Constraint Solver** | 🔴 Legacy | Rarely used, tests excluded | Consider removal |
| **`OpenL.getInstance()`** | ❌ Deprecated | Static singleton | Remove in 7.0.0 |
| **Operator methods in `Operators`** | ❌ Deprecated | 20+ methods | Use annotations instead |
| **Legacy source modules** | ❌ Deprecated | Multiple classes | Being removed |

### STUDIO Module

| Component | Status | Notes | Action Plan |
|-----------|--------|-------|-------------|
| **React UI** (`studio-ui`) | ✅ Modern | Active development | Primary UI |
| **REST API** | ✅ Modern | Production-ready | Continue using |
| **Repository Abstraction** | ✅ Modern | Multi-backend | Continue using |
| **Git Repository** | ✅ Modern | JGit-based | Continue using |
| **AWS S3 Repository** | ✅ Modern | Production-ready | Continue using |
| **Azure Blob Repository** | ⚠️ Transitional | TODO: Rewrite REST API | Improve |
| **Jackson Serialization** | ✅ Modern | Jackson 2.20.0 | Continue using |
| **Workspace Management** | ✅ Modern | Production-ready | Continue using |
| **Security Framework** | ✅ Modern | Spring Security 6.5.5 | Continue using |
| **JSF Pages** | 🔴 Legacy | Being replaced | **Bug fixes only** |
| **RichFaces Components** | 🔴 Legacy | Custom fork | **Bug fixes only** |
| **Table Editor** (JavaScript) | 🔴 Legacy | Being replaced | **Bug fixes only** |

### WSFrontend Module

| Component | Status | Notes | Action Plan |
|-----------|--------|-------|-------------|
| **RuleService Core** | ✅ Modern | Production-ready | Continue using |
| **REST Services** (CXF) | ✅ Modern | Production-ready | Continue using |
| **SOAP Services** (CXF) | ⚠️ Transitional | Still used but declining | Maintain |
| **Kafka Integration** | ✅ Modern | Production-ready | Continue using |
| **OpenAPI Generation** | ✅ Modern | Swagger integration | Continue using |
| **Request Logging** | ✅ Modern | Database-backed | Continue using |

### Util Module

| Component | Status | Notes | Action Plan |
|-----------|--------|-------|-------------|
| **Maven Plugin** | ✅ Modern | Production-ready | Continue using |
| **Archetypes** | ✅ Modern | Quick start | Continue using |
| **OpenAPI Tools** | ✅ Modern | Code generation | Continue using |
| **Profiler** | ✅ Modern | Performance analysis | Continue using |
| **OpenTelemetry** | ✅ Modern | Observability | Continue using |

---

## Legacy Technologies

### 1. JSF/RichFaces (STUDIO)

**Status**: 🔴 **Legacy - Being Replaced**

**Location**:
- `/STUDIO/org.openl.rules.webstudio/src/main/webapp/`
- JSF pages (`.xhtml`)
- Managed beans

**Why Legacy**:
- Server-side rendering (slow)
- Limited client-side interactivity
- Difficult to maintain
- RichFaces abandoned by upstream

**Migration Path**:
```
JSF/RichFaces → React 18.3.1 + TypeScript 5.8.3
```

**Current Status**:
- React UI covers most features
- JSF still used for some legacy pages
- Table editor being migrated

**Action**:
- **Do not add new JSF pages**
- **Bug fixes only for existing JSF**
- **New features in React**
- Target: Complete migration by v7.0

### 2. RichFaces Custom Fork

**Status**: 🔧 **Custom Fork - Maintained by OpenL**

**Location**: External dependency

**Why Forked**:
- RichFaces abandoned by JBoss
- Required Jakarta EE migration
- OpenL-specific customizations

**Maintenance Burden**:
- Manual security updates
- Jakarta namespace migration
- Bug fixes without upstream

**Action**:
- Maintain until JSF removal complete
- No new features
- Plan deprecation with React migration

### 3. Table Editor (JavaScript/jQuery)

**Status**: 🔴 **Legacy - Being Replaced**

**Location**: `/STUDIO/org.openl.rules.tableeditor/`

**Why Legacy**:
- jQuery-based (old patterns)
- Hard to maintain
- Limited features
- Browser compatibility issues

**Migration Path**:
```
jQuery Table Editor → React Table Editor
```

**Action**:
- Bug fixes only
- Complete React replacement
- Remove jQuery dependencies

### 4. SOAP Web Services

**Status**: ⚠️ **Transitional - Declining Usage**

**Location**: `/WSFrontend/org.openl.rules.ruleservice.ws/`

**Why Transitional**:
- Industry moving to REST
- SOAP still required by legacy clients
- More overhead than REST

**Action**:
- Maintain for backward compatibility
- Prioritize REST for new features
- Document SOAP as legacy option

### 5. Constraint Solver

**Status**: 🔴 **Legacy - Consider Removal**

**Location**: `/DEV/org.openl.rules.constrainer/`

**Why Legacy**:
- Rarely used in practice
- Tests excluded from build (broken)
- Maintenance burden
- Better alternatives available

**Action**:
- Evaluate usage in customer projects
- If not used, schedule removal
- If used, fix tests and document

---

## Deprecated APIs

### DEV Module

**1. OpenL.getInstance()**
```java
// ❌ Deprecated - DO NOT USE
OpenL openL = OpenL.getInstance();

// ✅ Modern alternative
OpenLBuilder builder = new OpenLBuilder();
OpenL openL = builder.build();
```

**Reason**: Static singleton anti-pattern, configuration inflexibility

**Removal**: Planned for v7.0.0

---

**2. Operator methods in Operators class**
```java
// ❌ Deprecated - 20+ methods
public class Operators {
    @Deprecated
    public static int add(int a, int b) { ... }
}

// ✅ Modern alternative - use annotations
@Operator
public static int add(int a, int b) { ... }
```

**Reason**: Annotation-based approach more flexible

**Removal**: Planned for v7.0.0

---

**3. Legacy Source Code Modules**
```java
// ❌ Deprecated
ModuleFileSourceCodeModule
CompositeSourceCodeModule
SubTextSourceCodeModule
VirtualSourceCodeModule

// ✅ Modern alternatives
// Simpler source abstractions
```

**Reason**: Overcomplicated abstractions

**Removal**: Planned for v6.1.0

---

## Migration Status by Feature Area

### User Interface

| Feature | Legacy (JSF) | Modern (React) | Status |
|---------|-------------|----------------|---------|
| **Dashboard** | ✅ Exists | ✅ Exists | ⚠️ Migrated, JSF fallback |
| **Project List** | ✅ Exists | ✅ Exists | ⚠️ Migrated, JSF fallback |
| **Rule Editor** | ✅ Exists | 🚧 In Progress | ⚠️ Migrating |
| **Test Runner** | ✅ Exists | ✅ Exists | ⚠️ Migrated |
| **Settings** | ✅ Exists | ✅ Exists | ⚠️ Migrated |
| **User Management** | ✅ Exists | 🚧 Planned | 🔴 JSF only |
| **Repository Config** | ✅ Exists | 🚧 Planned | 🔴 JSF only |
| **Table Editor** | ✅ Exists (jQuery) | 🚧 In Progress | 🔴 Legacy |

**Migration Progress**: ~60% complete

---

### Repository Backends

| Backend | Status | Notes |
|---------|--------|-------|
| **File System** | ✅ Modern | Production-ready |
| **Git (JGit)** | 🔧 Custom Fork | Maintained by OpenL |
| **Database** | ✅ Modern | H2, PostgreSQL, MySQL, etc. |
| **AWS S3** | ✅ Modern | Production-ready |
| **Azure Blob** | ⚠️ Needs Improvement | TODO: Rewrite REST API |
| **ZIP/JAR** | ✅ Modern | Read-only archives |

---

### Authentication

| Method | Status | Notes |
|--------|--------|-------|
| **Standalone** | ✅ Modern | File-based |
| **SAML** | ✅ Modern | OpenSAML 5.1.6 |
| **CAS** | ✅ Modern | Production-ready |
| **JWT** | ✅ Modern | Token-based |
| **ACL** | ✅ Modern | Spring Security ACL |

---

## Technical Debt Inventory

### High Priority (Critical)

**1. JSF/RichFaces Removal**
- **Impact**: High
- **Effort**: Large (6-12 months)
- **Risk**: Medium (user disruption)
- **Action**: Complete React migration

**2. Fix Constraint Solver Tests**
- **Impact**: Medium
- **Effort**: Small (1 week)
- **Risk**: Low
- **Action**: Fix or remove

**3. Cache Implementation Review**
- **Impact**: High (memory leaks)
- **Effort**: Medium (1 month)
- **Risk**: High
- **Action**: Add eviction policy, review all caches

### Medium Priority (Important)

**4. String Interning Review**
- **Impact**: Medium
- **Effort**: Medium
- **Risk**: Medium
- **Action**: Profile and optimize

**5. Azure Repository Rewrite**
- **Impact**: Medium
- **Effort**: Medium
- **Risk**: Medium
- **Action**: Use REST API directly

**6. Generic Type Support**
- **Impact**: Medium (feature gap)
- **Effort**: Large
- **Risk**: High
- **Action**: Add Java generics support

### Low Priority (Nice to Have)

**7. Public Field Refactoring**
- **Impact**: Low
- **Effort**: Small
- **Risk**: Low
- **Action**: Make fields private with getters

**8. Method Overload Resolution Optimization**
- **Impact**: Low (performance)
- **Effort**: Medium
- **Risk**: Medium
- **Action**: Optimize algorithm

---

## Custom Forks Maintained by OpenL

### 1. RichFaces

**Upstream**: Abandoned by JBoss/Red Hat
**Fork**: Maintained by OpenL Team
**Version**: 10.0.0 (OpenL custom)

**Modifications**:
- Jakarta EE namespace migration
- Security patches
- Bug fixes
- OpenL-specific customizations

**Maintenance**:
- Manual security updates
- No new features
- Plan deprecation

### 2. JGit

**Upstream**: Eclipse JGit
**Fork**: Custom enhancements by OpenL
**Version**: 7.3.0 (OpenL custom)

**Modifications**:
- OpenL-specific optimizations
- Custom merge algorithms
- Excel conflict detection

**Maintenance**:
- Periodic upstream merges
- OpenL-specific features
- Long-term maintenance

### 3. Flyway

**Upstream**: Redgate Flyway
**Fork**: Custom version
**Version**: 4.2.0.3 (OpenL custom)

**Modifications**:
- Database schema customizations
- Migration script enhancements

**Maintenance**:
- Minimal changes
- Consider upstream upgrade

---

## Migration Guidelines

### When to Migrate

**Migrate immediately**:
- Security vulnerabilities
- Breaking changes in dependencies
- Performance issues

**Migrate in next release**:
- Deprecated APIs
- Legacy patterns
- Technical debt

**Defer migration**:
- Working code with no issues
- Low-priority features
- Risk > benefit

### How to Migrate

**1. Assess Impact**
```
- Who uses this component?
- What depends on it?
- What's the migration path?
- What's the risk?
```

**2. Plan Migration**
```
- Create feature branch
- Implement new version
- Run tests in parallel
- Gradual rollout
```

**3. Execute Migration**
```
- Implement replacement
- Add deprecation warnings
- Update documentation
- Communicate to users
```

**4. Deprecation Period**
```
- Maintain both versions (1-2 releases)
- Warn users
- Provide migration guide
- Monitor usage
```

**5. Remove Legacy**
```
- Remove deprecated code
- Update dependencies
- Update documentation
- Announce removal
```

---

## Modernization Roadmap

### Version 6.1 (Next Minor)

**Goals**:
- Complete React migration (high-priority features)
- Remove deprecated source modules
- Fix constraint solver tests
- Optimize caching

**Timeline**: 3-6 months

### Version 7.0 (Next Major)

**Goals**:
- Remove JSF/RichFaces completely
- Remove deprecated APIs (`OpenL.getInstance()`, etc.)
- Complete React table editor
- Generic type support

**Timeline**: 12-18 months

**Breaking Changes**:
- JSF pages removed
- Deprecated APIs removed
- Configuration changes
- API changes in core

### Version 8.0 (Future)

**Goals**:
- Modern architecture patterns
- Microservices-ready
- Cloud-native features
- Enhanced observability

**Timeline**: 24+ months

---

## Decision Criteria

### Should I Use This Component?

**✅ Use if**:
- Status: Modern (✅)
- Status: Transitional (⚠️) - but prefer modern alternative
- Well-documented
- Actively maintained
- No known issues

**⚠️ Caution if**:
- Status: Transitional (⚠️)
- Limited documentation
- Known issues
- Custom fork

**❌ Avoid if**:
- Status: Legacy (🔴)
- Status: Deprecated (❌)
- Scheduled for removal
- Security vulnerabilities
- Not maintained

---

## See Also

- [Technology Stack](/docs/architecture/technology-stack.md) - Complete technology list
- [Dependencies](/docs/architecture/dependencies.md) - Dependency details
- [Development Setup](/docs/onboarding/development-setup.md) - Setup guide
- [CLAUDE.md](/CLAUDE.md) - Coding conventions

---

**Last Updated**: 2025-11-05
**Review Cycle**: Quarterly
**Next Review**: 2025-02-05

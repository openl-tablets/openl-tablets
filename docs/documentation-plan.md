# OpenL Tablets Documentation Plan

**Repository**: OpenL Tablets
**Version**: 6.0.0-SNAPSHOT
**Total Modules**: 50+ submodules
**Documentation Started**: 2025-11-05

---

## Project Overview

OpenL Tablets is an enterprise-grade business rules engine with:
- **Core Rules Engine** (DEV module) - 9 submodules
- **Web Studio & Management** (STUDIO module) - 22 submodules
- **Web Services & Rule Deployment** (WSFrontend module) - 12 submodules
- **Utilities & Tools** (Util module) - 9 submodules
- **Integration Tests** (ITEST module) - 18+ test modules
- **Demo Application** (DEMO module)

**Technology Stack**: Java 21+, Spring Boot 3.5.6, React 18.3.1, TypeScript, Maven

---

## Documentation Structure

```
docs/
├── documentation-plan.md           # This file
├── module-docs-progress.md         # Progress tracking (checkpoint)
├── claude-workflows.md             # Claude Code usage patterns
├── architecture/
│   ├── legacy-system-map.md       # Map of all code
│   ├── migration-status.md        # Modernization status
│   ├── dependencies.md            # Dependency relationships
│   └── technology-stack.md        # Technology overview
├── onboarding/
│   ├── codebase-tour.md           # Quick start guide
│   ├── common-tasks.md            # Frequent operations
│   └── development-setup.md       # Developer setup
├── plans/
│   ├── current-phase.md           # Active work
│   └── technical-debt.md          # Known issues
└── analysis/
    ├── dev-module-overview.md     # DEV module analysis
    ├── studio-module-overview.md  # STUDIO module analysis
    ├── wsfrontend-module-overview.md  # WSFrontend analysis
    └── util-module-overview.md    # Util module analysis
```

---

## Module-Specific CLAUDE.md Files

Each major module will have a CLAUDE.md file with module-specific conventions:

```
openl-tablets/
├── CLAUDE.md                                # Root conventions
├── DEV/
│   └── CLAUDE.md                           # Core engine rules
├── STUDIO/
│   ├── CLAUDE.md                           # Studio conventions
│   └── studio-ui/
│       └── CLAUDE.md                       # React/TypeScript patterns
├── WSFrontend/
│   └── CLAUDE.md                           # Web services conventions
└── Util/
    └── CLAUDE.md                           # Utility conventions
```

---

## Batch Organization

### **BATCH 1: Core Architecture & Foundation** ⏳ PENDING
**Status**: Not started
**Estimated Modules**: Foundation + 9 DEV modules
**Focus**: Core rules engine, project structure, common utilities

#### Modules in Batch 1:
1. **Foundation Documents**
   - `/docs/architecture/technology-stack.md`
   - `/docs/architecture/legacy-system-map.md`
   - `/docs/architecture/dependencies.md`
   - `/docs/onboarding/codebase-tour.md`
   - `/docs/onboarding/development-setup.md`
   - `/CLAUDE.md` (root)

2. **DEV Module** (`/home/user/openl-tablets/DEV/`)
   - `org.openl.commons` - Common utilities and base classes
   - `org.openl.rules` - **Core rules engine** (MAIN)
   - `org.openl.rules.annotations` - Rule annotations API
   - `org.openl.rules.util` - Rules-specific utilities
   - `org.openl.rules.gen` - Code generation framework
   - `org.openl.rules.constrainer` - Constraint solver
   - `org.openl.rules.project` - Project management framework
   - `org.openl.spring` - Spring framework integration
   - `org.openl.rules.test` - Functional test utilities
   - `/DEV/CLAUDE.md` - Core engine conventions

**Deliverables**:
- `/docs/analysis/dev-module-overview.md`
- `/docs/architecture/dependencies.md` (DEV module dependencies)
- `/DEV/CLAUDE.md`

---

### **BATCH 2: Repository & Workspace Layer** ⏳ PENDING
**Status**: Not started
**Estimated Modules**: 8 repository/workspace modules
**Focus**: Data persistence, version control, cloud storage

#### Modules in Batch 2:
1. **Repository Infrastructure** (`/home/user/openl-tablets/STUDIO/`)
   - `org.openl.rules.repository` - Base repository abstraction
   - `org.openl.rules.repository.git` - Git repository implementation
   - `org.openl.rules.repository.aws` - AWS S3 repository support
   - `org.openl.rules.repository.azure` - Azure Blob repository support
   - `org.openl.rules.workspace` - Workspace management

2. **Serialization & Data**
   - `org.openl.rules.jackson` - Jackson serialization
   - `org.openl.rules.jackson.configuration` - Jackson configuration

3. **Utilities**
   - `org.openl.rules.diff` - Diff calculation
   - `org.openl.rules.xls.merge` - Excel merge utilities

**Deliverables**:
- `/docs/analysis/repository-layer-overview.md`
- Module-specific documentation in each submodule

---

### **BATCH 3: Security & Authentication** ⏳ PENDING
**Status**: Not started
**Estimated Modules**: 3 security modules
**Focus**: Authentication, authorization, ACL, SAML

#### Modules in Batch 3:
1. **Security Framework** (`/home/user/openl-tablets/STUDIO/`)
   - `org.openl.security` - Security framework base
   - `org.openl.security.standalone` - Standalone security implementation
   - `org.openl.security.acl` - ACL-based security

**Deliverables**:
- `/docs/analysis/security-overview.md`
- `/docs/architecture/security-architecture.md`
- Security-specific CLAUDE.md guidelines

---

### **BATCH 4: Web Studio Core** ⏳ PENDING
**Status**: Not started
**Estimated Modules**: 4 studio core modules
**Focus**: Web application, table editor, OpenAPI generation

#### Modules in Batch 4:
1. **Studio Core** (`/home/user/openl-tablets/STUDIO/`)
   - `org.openl.rules.webstudio` - Main Web Studio (WAR)
   - `org.openl.rules.webstudio.web` - Web utilities
   - `org.openl.rules.webstudio.ai` - AI features
   - `org.openl.rules.tableeditor` - Table editor component

**Deliverables**:
- `/docs/analysis/webstudio-core-overview.md`
- `/STUDIO/CLAUDE.md`

---

### **BATCH 5: Studio Frontend** ⏳ PENDING
**Status**: Not started
**Estimated Modules**: 1 large TypeScript/React module
**Focus**: Modern React UI, TypeScript components

#### Modules in Batch 5:
1. **Frontend Application** (`/home/user/openl-tablets/STUDIO/`)
   - `studio-ui` - React/TypeScript frontend
     - Component structure
     - State management (Zustand)
     - Routing (React Router)
     - UI framework (Ant Design)
     - Internationalization (i18next)
     - Build configuration (Webpack)

**Deliverables**:
- `/docs/analysis/studio-ui-overview.md`
- `/STUDIO/studio-ui/CLAUDE.md` (React/TypeScript conventions)
- Component documentation

---

### **BATCH 6: OpenAPI & Code Generation** ⏳ PENDING
**Status**: Not started
**Estimated Modules**: 3 OpenAPI modules
**Focus**: OpenAPI generation, validation, Spring integration

#### Modules in Batch 6:
1. **OpenAPI Framework** (`/home/user/openl-tablets/STUDIO/`)
   - `org.openl.rules.project.openapi` - OpenAPI generation
   - `org.openl.rules.project.validation.openapi` - OpenAPI validation
   - `org.openl.rules.spring.openapi` - Spring OpenAPI integration

**Deliverables**:
- `/docs/analysis/openapi-framework-overview.md`

---

### **BATCH 7: Rule Services Core** ⏳ PENDING
**Status**: Not started
**Estimated Modules**: 12 rule service modules
**Focus**: Rule deployment, web services, Kafka integration

#### Modules in Batch 7:
1. **Rule Service Infrastructure** (`/home/user/openl-tablets/WSFrontend/`)
   - `org.openl.rules.ruleservice` - Core rule service
   - `org.openl.rules.ruleservice.annotation` - Service annotations
   - `org.openl.rules.ruleservice.common` - Common service utilities
   - `org.openl.rules.ruleservice.deployer` - Service deployer

2. **Web Services**
   - `org.openl.rules.ruleservice.ws` - Web Services (WAR)
   - `org.openl.rules.ruleservice.ws.all` - WS with all plugins
   - `org.openl.rules.ruleservice.ws.common` - WS common utilities
   - `org.openl.rules.ruleservice.ws.annotation` - WS annotations

3. **Kafka Integration**
   - `org.openl.rules.ruleservice.kafka` - Kafka message support

4. **Logging & Storage**
   - `org.openl.rules.ruleservice.ws.storelogdata` - Log storage abstraction
   - `org.openl.rules.ruleservice.ws.storelogdata.db` - DB log storage
   - `org.openl.rules.ruleservice.ws.storelogdata.db.annotation` - Log annotations

**Deliverables**:
- `/docs/analysis/wsfrontend-module-overview.md`
- `/WSFrontend/CLAUDE.md`

---

### **BATCH 8: Utilities & Tools** ⏳ PENDING
**Status**: Not started
**Estimated Modules**: 9 utility modules
**Focus**: Maven plugins, archetypes, profiler, OpenTelemetry

#### Modules in Batch 8:
1. **Maven Tooling** (`/home/user/openl-tablets/Util/`)
   - `openl-maven-plugin` - Maven plugin for OpenL compilation
   - `openl-project-archetype` - Maven project archetype
   - `openl-simple-project-archetype` - Simple Maven archetype

2. **OpenAPI Tooling**
   - `openl-openapi-model-scaffolding` - OpenAPI code generator
   - `openl-openapi-parser` - OpenAPI parser

3. **Utilities**
   - `openl-excel-builder` - Excel file builder utility
   - `openl-yaml` - YAML support
   - `org.openl.rules.profiler` - Performance profiler
   - `openl-rules-opentelemetry` - OpenTelemetry support

**Deliverables**:
- `/docs/analysis/util-module-overview.md`
- `/Util/CLAUDE.md`

---

### **BATCH 9: Integration Tests & Demo** ⏳ PENDING
**Status**: Not started
**Estimated Modules**: 18+ test modules + DEMO
**Focus**: Integration testing, test infrastructure, demo app

#### Modules in Batch 9:
1. **Test Infrastructure** (`/home/user/openl-tablets/ITEST/`)
   - `server-core` - Shared test server infrastructure
   - `itest.smoke` - Smoke tests
   - `itest.webstudio` - WebStudio integration tests

2. **Security & Authentication Tests**
   - `itest.security` - Security tests
   - `itest.security.cas` - CAS authentication tests
   - `itest.security.saml` - SAML tests

3. **Service Tests**
   - `itest.webservice.rest` - REST service tests
   - `itest.webservice.soap` - SOAP service tests
   - `itest.kafka.smoke` - Kafka integration tests

4. **Infrastructure Tests**
   - `itest.spring-boot` - Spring Boot tests
   - `itest.minio` - MinIO storage tests
   - `itest.health` - Health check tests
   - `itest.deployment-filters` - Deployment filter tests

5. **Demo Application**
   - `/DEMO/` - Demo application module

**Deliverables**:
- `/docs/analysis/itest-overview.md`
- `/docs/onboarding/testing-guide.md`
- `/ITEST/CLAUDE.md`

---

### **BATCH 10: Final Documentation & Refinement** ⏳ PENDING
**Status**: Not started
**Focus**: Cross-cutting concerns, migration guides, workflows

#### Deliverables:
1. **Architecture Documentation**
   - `/docs/architecture/migration-status.md` - Modernization tracking
   - `/docs/architecture/legacy-system-map.md` - Complete system map

2. **Workflows & Plans**
   - `/docs/claude-workflows.md` - How to use Claude Code with this repo
   - `/docs/plans/current-phase.md` - Current development phase
   - `/docs/plans/technical-debt.md` - Known technical debt

3. **Onboarding**
   - `/docs/onboarding/common-tasks.md` - Common development tasks
   - `/docs/onboarding/troubleshooting.md` - Common issues and solutions

4. **Final Review**
   - Review all CLAUDE.md files for consistency
   - Ensure all cross-references are correct
   - Validate documentation completeness

---

## Progress Tracking

| Batch | Status | Modules | Completion Date | Reviewer Approval |
|-------|--------|---------|----------------|-------------------|
| Batch 1: Core Architecture & Foundation | ⏳ PENDING | 10 | - | - |
| Batch 2: Repository & Workspace Layer | ⏳ PENDING | 8 | - | - |
| Batch 3: Security & Authentication | ⏳ PENDING | 3 | - | - |
| Batch 4: Web Studio Core | ⏳ PENDING | 4 | - | - |
| Batch 5: Studio Frontend | ⏳ PENDING | 1 | - | - |
| Batch 6: OpenAPI & Code Generation | ⏳ PENDING | 3 | - | - |
| Batch 7: Rule Services Core | ⏳ PENDING | 12 | - | - |
| Batch 8: Utilities & Tools | ⏳ PENDING | 9 | - | - |
| Batch 9: Integration Tests & Demo | ⏳ PENDING | 18+ | - | - |
| Batch 10: Final Documentation & Refinement | ⏳ PENDING | N/A | - | - |

**Total Estimated Modules**: 68+
**Completed**: 0
**In Progress**: 0
**Pending**: 68+

---

## Documentation Standards

### For Each Module:
1. **Purpose and Behavior**: What does this module do? Why does it exist?
2. **Important Concepts**: Key classes, interfaces, patterns
   - Type information
   - Description
   - Constraints and limitations
3. **Side Effects**: Database modifications, file I/O, network calls
4. **Dependencies**:
   - Internal dependencies (other OpenL modules)
   - External dependencies (third-party libraries)
5. **Usage Examples**: Non-obvious usage patterns
6. **Known Issues**: Technical debt, limitations, workarounds
7. **Entry Points**: Main classes, public APIs

### CLAUDE.md Guidelines:
- Module-specific coding conventions
- Testing requirements
- Common pitfalls
- "Don't touch" areas (legacy code)
- Modernization status

---

## Memory Checkpoints

After each batch completion:
1. Update this file with completion status
2. Update `/docs/module-docs-progress.md` with:
   - COMPLETED: List of documented files
   - NEXT BATCH: Preview of next modules
   - OPEN QUESTIONS: Unclear items requiring clarification
3. Commit changes to git
4. Wait for user approval before proceeding

---

## Next Steps

1. ✅ **COMPLETED**: Explore repository structure
2. ✅ **COMPLETED**: Create documentation plan
3. ⏳ **IN PROGRESS**: Set up documentation directory structure
4. ⏳ **PENDING**: Begin Batch 1 documentation
5. ⏳ **PENDING**: Wait for user review and approval

---

**Plan Created**: 2025-11-05
**Last Updated**: 2025-11-05
**Total Estimated Time**: 10 batches × multiple sessions

# STUDIO Module — Web IDE

Spring Boot backend + React/TypeScript frontend (modern) + JSF/RichFaces (legacy, being replaced).

## Key Conventions

- **JSF pages** (`org.openl.rules.webstudio/src/main/webapp/`): Explicit bug fixes only. Do NOT improve legacy code or possible bugs. New features must go in React frontend.
- **New features** → React in `studio-ui/`
- **DB migrations**: Flyway scripts in `org.openl.security.standalone/resources/db/flyway/`
- **Authentication**: Form-based, SAML, OAuth2, LDAP/AD, Personal Access Tokens
- **API documentation**: Use OpenAPI annotations on REST controllers
- **Response field projection**: Clients may add `?fields=id,name,modules(id,name)` to reduce JSON to selected fields, including nested objects/arrays (hierarchical, GraphQL-like). Applied globally during serialization (`org.openl.studio.common.projection`), so controllers need no extra parameter. Nested sub-selection only recurses into DTOs in the configured `*.model` packages (other nested types are returned whole); errors, binary and non-JSON responses are never touched. New response-DTO packages must be added to `openl.rest.field-projection.base-packages`; unknown-field handling is toggled with `openl.rest.field-projection.fail-on-unknown-field`.

## Submodules

**Core application**:
- **org.openl.rules.webstudio** — Main Spring Boot app (packages: `org.openl.studio.*`, `org.openl.rules.webstudio`, `org.openl.rules.rest`, `org.openl.rules.ui`)
- **org.openl.rules.webstudio.web** — Web layer components
- **org.openl.rules.webstudio.ai** — AI integration features
- **studio-ui/** — React/TypeScript frontend (see `studio-ui/AGENTS.md`)

**Repository & storage**:
- **org.openl.rules.repository** — Repository abstraction layer
- **org.openl.rules.repository.git** — Git repository implementation
- **org.openl.rules.repository.aws** — AWS S3 storage
- **org.openl.rules.repository.azure** — Azure Blob storage

**Security** (package: `org.openl.studio.security`):
- **org.openl.security** — Security abstractions
- **org.openl.security.standalone** — Standalone auth (form-based, DB-backed, Flyway migrations in `resources/db/flyway/`)
- **org.openl.security.acl** — Access Control Lists

**Supporting modules**:
- **org.openl.rules.tableeditor** — Table editor component
- **org.openl.rules.workspace** — Workspace management
- **org.openl.rules.diff** — Rule diff/comparison
- **org.openl.rules.demo** — Demo projects
- **org.openl.rules.jackson** / **org.openl.rules.jackson.configuration** — JSON serialization
- **org.openl.rules.project.openapi** / **org.openl.rules.project.validation.openapi** — OpenAPI generation and validation
- **org.openl.rules.spring.openapi** — Spring OpenAPI integration
- **org.openl.rules.xls.merge** — Excel merge utilities

## Backend Package Structure

```
org.openl.studio
├── config/          # Spring Boot configuration
├── security/        # Auth: AD, OAuth2, SAML, PAT (personal access tokens)
├── settings/        # System settings (rest/controller, service, model, converter)
├── notification/    # WebSocket notifications
├── projects/        # Project management and validation
├── repositories/    # Repository management
├── deployment/      # Deployment management
├── users/           # User management
├── tags/            # Tag management
├── rest/            # REST controllers
├── socket/          # WebSocket endpoints
└── common/          # Shared utilities
```

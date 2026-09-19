# STUDIO Module — Web IDE

Spring Boot backend serving a REST API, and a React/TypeScript frontend that draws every screen.

## Key Conventions

- **The server renders no page.** Every address answers with the one page the frontend build wrote
  (`StaticResourcesServlet`), and the screen is drawn in the browser; everything else the server answers is REST or
  a WebSocket message. A feature therefore lands as an endpoint plus a React screen — never as a server-rendered
  page or a fragment of HTML.
- **ACL checks are the last thing a condition evaluates.** A permission probe reaches the ACL database
  through a transaction of its own, while the project state it is weighed against is already at hand, so a
  condition tests the state first and asks for the permission only when the answer still depends on it.
  Compute a permission lazily — never up front "in case it is needed" — and reuse one answer across every
  condition that weighs it (`ProjectAccessService.computeCapabilities` is the worked example). The same
  applies in the large: never ask per branch, per file or per artefact what one question about the project
  answers, and never repeat a question a pass over the same set has already answered.
- **New features** → React in `studio-ui/`
- **DB migrations**: Liquibase change logs in `org.openl.security.standalone/resources/db/changelog/`,
  baselined at the schema of 6.0.0 — see [`org.openl.security.standalone/AGENTS.md`](org.openl.security.standalone/AGENTS.md)
- **Authentication**: Form-based, SAML, OAuth2, LDAP/AD, Personal Access Tokens
- **REST API / OpenAPI**: Externalized descriptions, `@Parameter` vs `@Schema`, enum wire codes, and request
  validation follow strict rules — see [`org.openl.rules.webstudio/AGENTS.md`](org.openl.rules.webstudio/AGENTS.md)
- **Response field projection**: Clients add `?fields=id,name,modules(id,name)` to reduce JSON to selected fields, including nested objects and arrays (hierarchical, GraphQL-like). Applied globally during serialization (`org.openl.studio.common.projection`) — no controller-side parameter, no configuration. Any DTO under `org.openl.rules.*` or `org.openl.studio.*` is projectable, except framework infrastructure in `org.openl.studio.common.model` (errors, pagination wrappers). Errors, binary and non-JSON responses are never touched. OpenAPI integration lives separately in `org.openl.studio.openapi` and registers itself when the projection feature is present.

## Submodules

**Core application**:
- **org.openl.rules.webstudio** — Main Spring Boot app (packages: `org.openl.studio.*`, `org.openl.rules.webstudio`, `org.openl.rules.rest`, `org.openl.rules.ui`)
- **org.openl.rules.webstudio.web** — Web layer components
- **studio-ui/** — React/TypeScript frontend (see `studio-ui/AGENTS.md`)

**Repository & storage**:
- **org.openl.rules.repository** — Repository abstraction layer
- **org.openl.rules.repository.git** — Git repository implementation
- **org.openl.rules.repository.aws** — AWS S3 storage
- **org.openl.rules.repository.azure** — Azure Blob storage

**Security** (package: `org.openl.studio.security`):
- **org.openl.security** — Security abstractions
- **org.openl.security.standalone** — Standalone auth (form-based, DB-backed, Liquibase change logs in `resources/db/changelog/`)
- **org.openl.security.acl** — Access Control Lists

**Supporting modules**:
- **org.openl.rules.tableeditor** — Table layout and cell editor model read by the table REST API
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

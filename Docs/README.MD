# OpenL Tablets Documentation

The documentation of OpenL Tablets: the rules engine, OpenL Studio, and OpenL Rule Services. The folder is published at
<https://openl-tablets.github.io/openl-tablets/>, and the [user guides](user-guides/index.md) are also shipped inside OpenL
Studio.

## Using OpenL Tablets

- [What Is OpenL Tablets](what-is-openl-tablets.md) and [Features](features.md) — what the product does.
- [User Guides](user-guides/index.md) — installation, the demo package, OpenL Studio, OpenL Rule Services, and the
  reference guide of the language and the table types.
- [Supported Platforms](supported-platforms.md) — Java, application servers, databases, and browsers.
- [Release Notes](release-notes/README.md) and [Migration Notes](migration-notes.md) — what changed in each release.

## Deploying and Integrating

- [Deployment](DEPLOYMENT.md) — the Docker images, Docker Compose, Kubernetes, and application servers.
- [Production Deployment](Production_Deployment.md) — the roles and the stages of a production pipeline.
- [Examples](examples/index.md) — Docker Compose, Maven, and Kubernetes examples.
- [Configuration](configuration/index.md) — where the settings are described, and the security of Rule Services.
- [Integration Guides](integration-guides/index.md) — Spring, OpenAPI, Apache CXF, and OpenTelemetry in Rule Services.
- [REST API](api/README.md) — the table, merge, trace, history, and token APIs of OpenL Studio, and the admin API of
  Rule Services.

## Writing Rules Projects

- [Developer Guide](developer-guides/index.md) — rules projects, the business language, externalized configuration, and
  invoking rules from Java.
- Reference sheets: [Table Types](ref/table-types.md), [Rules Project Structure](ref/project-structure.md),
  [rules.xml](ref/rules.xml.md), [rules-deploy.xml](ref/rules-deploy.xml.md), and the
  [OpenL Maven Plugin](ref/openl-maven-plugin.md).

## Contributing to OpenL Tablets

- [Development Setup](onboarding/development-setup.md) and [Codebase Tour](onboarding/codebase-tour.md) — build the
  repository and find the code.
- [Architecture](ARCHITECTURE.md) — the engine, OpenL Studio, and Rule Services, with the pages that describe the
  details: [Technology Stack](architecture/technology-stack.md), [Dependencies](architecture/dependencies.md),
  [Client Sessions](architecture/client-sessions.md), [Cross-Branch Projects](architecture/cross-branch-projects.md),
  [Project Editing Lock](architecture/project-editing-lock.md),
  [WebSocket Change Notifications](architecture/websocket-change-notifications.md),
  [Workspace Metainfo Registry](architecture/workspace-metainfo-registry.md),
  [Table Creation](architecture/table-creation.md), [Table Dependency Graph](architecture/table-dependency-graph.md),
  [Decision Table Condition Indexing](architecture/decision-table-condition-indexing.md),
  [Upload Integrity](architecture/upload-integrity.md),
  [Embedded User Guides](architecture/embedded-user-guides.md), and the design of the
  [Built-in MCP Server](architecture/mcp-server-jetty-architecture.md).
- [Release Checklist](release.md) — the steps of a release.
- The conventions of the code are in the `AGENTS.md` files: [repository](../AGENTS.md), [DEV](../DEV/AGENTS.md),
  [STUDIO](../STUDIO/AGENTS.md), [studio-ui](../STUDIO/studio-ui/AGENTS.md), and
  [WSFrontend](../WSFrontend/AGENTS.md). The conventions of this folder are in [AGENTS.md](AGENTS.md).

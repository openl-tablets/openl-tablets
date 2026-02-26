## Release Notes

[v6.0.0](https://github.com/openl-tablets/openl-tablets/releases/tag/6.0.0) on the GitHub

OpenL Tablets 6.0.0 is a major release that modernizes the platform foundation and introduces AI-powered rule authoring. The entire administration interface has been rebuilt as a React single-page application, replacing the legacy JSF screens with a modern, responsive experience. The platform now requires Java 21 and has migrated from Java EE 8 to Jakarta EE 10, with Spring Boot upgraded to 3.5 and Spring Framework to 6.2.

Key highlights include a built-in MCP (Model Context Protocol) server that allows AI assistants to create, edit, test, and deploy business rules directly through OpenL Studio, a simplified three-role access control model (Viewer, Contributor, Manager), Personal Access Tokens for API and CI/CD authentication, and comprehensive branch merge conflict resolution with a visual step-by-step wizard.

### New Features

Redesigned Administration Panel

The entire OpenL Studio administration interface has been rebuilt using React 19, TypeScript, and Ant Design 6. The new single-page application replaces the legacy JSF-based admin screens and provides a modern, responsive experience for managing all platform settings. Administration sections include system configuration, security settings, repository management, user and group management, notification preferences, tag configuration, and email settings. The new interface is fully internationalized with i18next and uses Zustand for state management.

![Image](images/admin-panel.png)

MCP Server for AI-Assisted Rule Authoring

OpenL Studio now includes a built-in Model Context Protocol (MCP) server that enables AI assistants to interact with the platform programmatically. The MCP integration exposes a comprehensive set of tools for project and rule lifecycle management, allowing AI agents to list, open, and inspect projects; create, read, update, and append tables; run and validate tests; manage branches; and deploy projects to production. The server also includes guided prompts for common workflows such as creating rules, running tests, and deploying projects. OAuth 2.0 resource metadata discovery is supported via the standard `/.well-known/oauth-protected-resource` endpoint.

  * List, open, and inspect projects and repositories
  * Create, read, update, and append rule tables
  * Run tests with targeted table and range selection
  * Deploy and redeploy projects to production repositories
  * Browse project revision history and restore previous versions
  * Create branches and manage project status
  * Guided prompts for rule creation, test authoring, and deployment

Simplified Role-Based Access Control

The access control model has been streamlined from a complex permission matrix to three clear roles: Viewer (read-only access), Contributor (read, create, edit, and delete), and Manager (full control including administration). Permissions can be assigned at the repository root level or on individual projects, giving administrators fine-grained control with minimal complexity. The new access management UI provides dedicated tabs for Design Repositories, Deploy Repositories, and individual Projects, making it straightforward to configure who can access what.

  * Three intuitive roles: Viewer, Contributor, Manager
  * Repository-level and project-level permission assignment
  * Visual permission editor with per-repository and per-project tabs
  * Default group assignment with tooltip display on Users and Groups tabs

![Image](images/access-management.png)

Personal Access Tokens

Users can now generate Personal Access Tokens (PATs) for authenticating REST API calls, CI/CD pipelines, and automated scripts without sharing their credentials. Tokens are created through the administration panel with configurable expiration periods (7, 30, 60, 90 days, custom date, or no expiration). The token value is displayed only once at creation time and authenticated via the `Authorization: Token <value>` header. Token metadata including creation date and expiration status is stored in a dedicated database table with secure hashing.

![Image](images/personal-access-tokens.png)

Branch Merge Conflict Resolution

A new visual merge wizard guides users through branch synchronization with step-by-step conflict resolution. The merge modal allows selecting source and target branches, displays detected conflicts, and provides tools to resolve each conflicting file. Non-conflicting sheets are merged automatically, reducing manual resolution effort.

![Image](images/merge-conflict-resolution.png)

### Improvements

Platform Modernization:

  * Migrated from Java EE 8 to Jakarta EE 10 with Jakarta Servlet 6.0, Jakarta XML Bind 4.0, and Jakarta Mail 2.1
  * Upgraded to Java 21 as the minimum required JDK version
  * Upgraded to Spring Boot 3.5.10 and Spring Framework 6.2.15
  * Upgraded to Spring Security 6.5.7

OpenL Studio:

  * Added new REST API to retrieve the branch list for a project when the user lacks full repository permissions
  * Added real-time user profile synchronization when current user information is updated
  * Improved application redirect rules with a dedicated RedirectToRoot component and comprehensive tests
  * Added URL encoding for request URLs to prevent malformed path issues
  * Restricted group loading to administrators only for improved security and performance

Core:

  * Simplified the rules instantiation API by consolidating CommonRulesInstantiationStrategy into SimpleMultiModuleInstantiationStrategy
  * Replaced InstantiationStrategy usage with RulesEngineFactory and SimpleProjectEngineFactory for cleaner project loading
  * Removed unreachable code paths and reduced internal API visibility for better encapsulation
  * Improved testing coverage for multi-module projects

Rule Services:

  * Integrated RapiDoc as the API documentation viewer

### Fixed Bugs

OpenL Studio:

  * Fixed deployment failures from database and folder repositories when permissions are configured at the project level
  * Fixed trace execution breaking when a virtual source code module workbook had no sheets
  * Fixed UnsupportedOperationException when retrieving test results with the unpaged=true parameter
  * Fixed duplicate generic error messages appearing during form operations
  * Fixed arrow styling issues in the administration interface
  * Fixed synchronous role saving to prevent race conditions

OpenL Studio, Rule Services:

  * Fixed multiple CVE vulnerabilities through comprehensive dependency updates

### Updated Libraries

Runtime Dependencies:

  * Spring Boot 3.5.10
  * Spring Framework 6.2.15
  * Spring Security 6.5.7
  * Spring AI 1.1.2
  * Jackson 2.21.0
  * Apache CXF 4.1.4
  * Kafka 4.1.1
  * gRPC 1.79.0
  * OpenTelemetry 2.24.0
  * Log4j 2.25.3
  * SLF4j 2.0.17
  * ASM 9.9.1
  * Hibernate ORM 6.6.42.Final
  * Jetty 12.1.6
  * Netty 4.2.10.Final
  * AWS S3 SDK 2.41.24
  * Azure Blob Storage 12.33.2
  * H2 2.4.240
  * HikariCP 7.0.2
  * Apache POI 5.5.1
  * Groovy 4.0.30
  * Guava 33.5.0-jre
  * Swagger Core 2.2.42
  * Apache Commons Codec 1.21.0
  * Apache Commons Compress 1.28.0
  * Apache Commons IO 2.21.0
  * Apache Commons Lang 3.20.0
  * Nimbus JOSE+JWT 10.7
  * Bouncy Castle 1.83
  * Jakarta XML Bind API 4.0.5

Test Dependencies:

  * JUnit 6.0.2
  * Mockito 5.21.0
  * Awaitility 4.3.0
  * Byte Buddy 1.18.4
  * Testcontainers 2.0.3
  * Testcontainers Keycloak 4.1.1
  * Minio 8.6.0

Maven Plugins:

  * Maven Compiler Plugin 3.15.0
  * Maven Plugin API 3.9.12
  * Maven Plugin Plugin 3.15.2

Frontend Dependencies:

  * React 19.2
  * TypeScript 5.9
  * Ant Design 6.2
  * Zustand 5.0
  * i18next 25.8
  * Webpack 5.105

### Migration Notes

This is a major version upgrade. Review the following changes before migrating:

  * Java 21 is now the minimum required JDK version. Update your runtime environment before deploying 6.0.0.
  * The platform has migrated from Java EE 8 to Jakarta EE 10. All `javax.*` namespace references in custom code or configuration must be updated to `jakarta.*`.
  * Spring Boot has been upgraded from 2.x to 3.5. Review your custom Spring configurations for compatibility with Spring Boot 3.x and Spring Framework 6.x conventions.
  * The user database schema has been updated with a new `OpenL_PAT_Tokens` table. Back up the database before upgrading. Flyway migrations will apply the schema changes automatically.
  * The access control model has been simplified to three roles (Viewer, Contributor, Manager). Existing permission assignments may need to be reviewed and remapped to the new role model.
  * The legacy JSF administration pages are still available but are superseded by the new React-based administration interface. Plan to adopt the new UI as legacy screens will be removed in a future release.

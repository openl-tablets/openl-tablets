# OpenL Tablets 6.0.0 Release Notes

## Overview

OpenL Tablets 6.0.0 is a landmark release that modernizes the entire platform, delivering a completely redesigned administration experience, native AI integration, and a fully updated technology foundation. This is the most significant upgrade in the project's history, aligning OpenL Tablets with current enterprise Java standards and opening new possibilities for how teams build, manage, and deploy business rules.

The three defining themes of this release are a modern administration interface, AI-powered rule development, and platform modernization. The new React-based Admin Panel replaces legacy screens with a unified, intuitive experience for managing users, groups, security, and repositories. The Model Context Protocol (MCP) integration makes OpenL Tablets the first business rules engine that AI assistants can operate directly — creating rules, running tests, and deploying projects through natural language. Under the hood, the migration to Java 21, Jakarta EE 10, Spring Boot 3.5, and Hibernate 6.6 ensures long-term support, improved security, and compatibility with modern enterprise infrastructure.

Whether you are modernizing your deployment stack, enabling AI-assisted rule development, or simply looking for a more productive administration experience, OpenL Tablets 6.0.0 provides a solid, future-proof foundation for your business rules management.

## What's New

### Major Features

#### Redesigned Admin Panel and New Admin UI

Managing users, permissions, and system settings in previous versions of OpenL Tablets relied on legacy JSF-based screens that were difficult to navigate, slow to respond, and limited in functionality. Administrators often needed to switch between multiple pages to complete routine tasks like assigning repository permissions to a user or configuring authentication, and the interface did not provide clear visual feedback about the current state of the system.

OpenL Tablets 6.0.0 introduces a completely redesigned Admin Panel built with React and Ant Design. The new interface provides a unified left-side navigation menu that organizes all administration functions into clearly defined sections: System, Security, Users, Groups, Repositories, Notification, Tags, and Mail. Each section loads instantly in the browser without full page refreshes, and real-time validation helps administrators catch configuration errors before they are applied.

The **Users** tab provides a comprehensive view of all system users with at-a-glance status indicators — green dots for online users, warning icons for accounts with unsafe default passwords, and colored tags showing group memberships. Administrators can create, edit, and delete users directly from the interface, with a sliding drawer that reveals user details, repository access rights, and project-level permissions in a single view. The **Groups** tab supports both internal groups and groups synchronized from external directories such as Active Directory or SAML providers, with visual indicators distinguishing admin groups, default groups, and external groups.

**Key capabilities:**
- **Unified navigation** with role-aware menu items that adapt based on administrator permissions
- **Granular access control** with per-user and per-group repository and project permissions using a clear three-role model (Viewer, Contributor, Manager)
- **Security configuration** supporting five authentication modes — Single-User, Multi-User, Active Directory, SAML, and OAuth2/OIDC — all configurable from a single screen
- **Repository management** for design and deployment repositories with support for Git, AWS S3, Azure Blob Storage, and database backends
- **Real-time form validation** with inline error messages and confirmation dialogs for destructive operations

The new Admin Panel is available at the `/admin` path and is designed to be the primary administration interface going forward. The legacy JSF-based administration screens remain accessible for backward compatibility during the transition period.

![Redesigned Admin Panel](images/admin-panel.png)

#### AI Integration via Model Context Protocol (MCP)

Business rules development has traditionally been a manual, spreadsheet-driven process. Rule authors work in Excel, switch to OpenL Studio to compile and test, review errors, and iterate — a cycle that requires deep familiarity with both the rule syntax and the Studio interface. For teams exploring AI-assisted development, there was previously no standardized way for AI tools to interact with OpenL Tablets.

OpenL Tablets 6.0.0 introduces a built-in MCP server that exposes the full lifecycle of rule development — project management, table operations, testing, and deployment — as structured tools that AI assistants like Claude can invoke directly. The server is built on the Spring AI framework and communicates over a stateless HTTP transport at the `/mcp` endpoint, inheriting the same authentication and authorization model used by the REST API.

The MCP integration provides over 20 tools organized into four categories. **Project management** tools let AI assistants list, open, close, and save projects, create branches, and navigate project history. **Table operations** tools enable creating, reading, updating, and appending rules, decision tables, data tables, and other OpenL table types with full structure awareness. **Testing tools** run project tests and return detailed pass/fail results with pagination for large test suites. **Deployment tools** handle deploying and redeploying projects to production targets.

**Key capabilities:**
- **20+ MCP tools** covering projects, tables, tests, repositories, and deployments
- **12 contextual AI prompts** providing guidance for common tasks like creating rules, writing tests, deploying projects, and resolving errors
- **Full authentication support** including OAuth 2.0, JWT, and Personal Access Tokens
- **Error handling** that translates technical exceptions into user-friendly messages suitable for AI interpretation
- **Pagination and filtering** across all list operations for efficient handling of large projects

To complement the tools, the system provides 12 rich prompt templates that guide AI assistants through OpenL-specific workflows — from choosing the right decision table type, to constructing test data with custom datatypes, to following a pre-deployment checklist. These prompts ensure that AI-generated rules follow OpenL best practices and conventions.

![MCP Integration Architecture](images/mcp-integration.png)

#### Platform Modernization: Java 21, Jakarta EE 10, and Spring Boot 3.5

Enterprise deployments of OpenL Tablets have historically needed to maintain older Java runtimes and legacy application server configurations due to the platform's dependency on Java EE (javax) APIs and Spring Framework 5.x. As the Java ecosystem has moved forward, this created increasing friction with modern infrastructure, security scanning tools, and container orchestration platforms.

OpenL Tablets 6.0.0 completes a comprehensive platform modernization, upgrading every major framework and runtime dependency to current enterprise standards. The minimum Java version is now 21 (up from 11), providing access to modern language features, improved garbage collection, and years of Long-Term Support. The entire codebase has been migrated from the `javax.*` namespace to `jakarta.*` (Jakarta EE 10), affecting servlet APIs, JPA, XML binding, mail, and WebSocket interfaces. Spring Framework has been upgraded to 6.2, Spring Boot to 3.5, and Hibernate ORM to 6.6.

The web container has been upgraded from Jetty 9/11 to Jetty 12.1 with Jakarta EE 10 modules, and Docker images now use Eclipse Temurin with Java 25 JRE on Alpine Linux for a minimal, secure footprint. Built-in OpenTelemetry instrumentation provides production-grade observability out of the box, with configurable exporters for metrics, traces, and logs.

**Key capabilities:**
- **Java 21+ runtime** with modern language features and long-term support
- **Jakarta EE 10** across all APIs — servlet, JPA, mail, XML binding, WebSocket
- **Spring Boot 3.5.10** with Spring Framework 6.2.15 and Spring Security 6.5.7
- **Hibernate 6.6** with Jakarta Persistence 3.0 for improved query performance and type safety
- **Jetty 12.1** with ee10 modules for modern container support
- **OpenTelemetry 2.24.0** for built-in observability and distributed tracing
- **Secure Docker images** running as non-root with GPG-verified OpenTelemetry agent

This modernization ensures that OpenL Tablets runs cleanly on current enterprise infrastructure, passes security scans without legacy library warnings, and benefits from the performance and security improvements in modern Java runtimes.

#### Simplified Role-Based Access Control

The previous OpenL Tablets permissions model used a complex, fine-grained access control system that was difficult to configure, audit, and reason about. Permissions were scattered across multiple configuration layers, and when users inherited access through several group memberships, understanding their effective permissions required careful manual analysis. Administrators frequently reported that setting up correct access for new team members was one of the most time-consuming and error-prone tasks, and misconfigured permissions led to either overly permissive access or unnecessary roadblocks for legitimate users.

OpenL Tablets 6.0.0 replaces this system with a fundamentally simpler three-role model built on Spring Security ACL. Every user's access to a repository or project is expressed as one of three roles: **Viewer** (read-only access to rules and test results), **Contributor** (read and write access to create, edit, and test rules), or **Manager** (full control including the ability to manage access for other users). Each repository has a configurable default role, and administrators can override access at the repository or individual project level when finer control is needed.

The new model is fully integrated with the redesigned Admin Panel. The **Users** tab shows each user's effective access across all repositories at a glance, while the **Groups** tab lets administrators assign roles to entire teams in a single operation. When a user's access is changed, the update takes effect immediately without requiring a session restart. New REST API endpoints under `/acls` expose the same capabilities programmatically, enabling automated provisioning workflows and integration with identity management systems.

**Key capabilities:**
- **Three clear roles** — Viewer, Contributor, and Manager — that eliminate ambiguity about what each user can do
- **Repository-level defaults** with per-project overrides for organizations that need targeted exceptions
- **Group-based assignment** that works with both internal groups and external directory groups (Active Directory, SAML, OAuth2)
- **Immediate effect** — permission changes apply instantly without requiring users to log out and back in
- **Audit-friendly** — the simplified model makes compliance reviews and access audits straightforward

This change is especially valuable for organizations with large teams, frequent onboarding, or regulatory requirements around access control. By reducing the permissions model to three well-defined roles, OpenL Tablets 6.0.0 makes it practical for any administrator — not just platform specialists — to manage access confidently and correctly.

![Simplified Access Control](images/access-control.png)

### Enhancements

#### Personal Access Tokens

Integrating OpenL Tablets with CI/CD pipelines, scripts, and external tools previously required sharing user credentials or configuring complex OAuth flows. This was a security concern and a source of friction for automation workflows.

You can now generate Personal Access Tokens (PATs) from the new Admin Panel under **My Settings**. Each token provides secure, scoped API access without exposing user passwords. Tokens can be created, listed, and revoked through both the UI and the REST API, making them suitable for automated deployment pipelines, monitoring integrations, and programmatic rule management.

#### Rules Description for OpenAPI

OpenL Tablets now supports adding descriptive annotations to rules that are automatically included in generated OpenAPI (Swagger) documentation. When you add descriptions to your rules, they appear in the API schema alongside parameter types and return values, making it easier for API consumers to understand what each rule does without consulting separate documentation.

#### Tags from Project Name

You can now extract tags automatically from project names using configurable project name templates. This feature is particularly useful for organizations that encode metadata in project naming conventions — such as department, region, or rule category. Tags are used for filtering and organizing projects in both the Studio interface and the MCP tools, enabling faster project discovery in large repositories.

#### Simplified Rule Engine Instantiation

The internal factory architecture for compiling and loading rule projects has been significantly simplified. The previous multi-strategy instantiation system — which included `CommonRulesInstantiationStrategy`, `ApiBasedInstantiationStrategy`, and `SimpleMultiModuleInstantiationStrategy` — has been consolidated into two clean entry points: `RulesEngineFactory` for single-module projects and `SimpleProjectEngineFactory` for multi-module projects. Unreachable code paths have been removed and the module reference chain has been flattened, resulting in more predictable compilation behavior and easier troubleshooting.

#### Branch Management API

A new REST API for branch management enables programmatic creation and listing of project branches. You can create branches from specific revisions, tags, or other branches, and list available branches with their protection status. This API is also exposed through the MCP tools, enabling AI-assisted version management workflows.

### Additional Improvements

- Added default group indicators with contextual tooltips on the Users and Groups tabs, helping administrators understand which group applies to new users by default
- Improved form validation messages and styling throughout the Admin Panel with clearer error descriptions and visual feedback
- Added context-sensitive button labels in user management — the save button now reads "Create" for new users and "Save" for existing users
- Enhanced user profile synchronization so that changes to the current user's details are immediately reflected in the profile context without requiring a page refresh
- Improved error notification handling to prevent duplicate messages when multiple operations fail simultaneously
- Added support for encoding request URLs to handle special characters in project and resource names
- Enhanced redirect handling with a dedicated component for cleaner application routing and navigation
- Pinned npm version in the build configuration to prevent spurious changes to the package lock file during builds

## Bug Fixes

### OpenL Studio

- Fixed an issue where the Trace and Run functionalities would fail when working with input objects containing more than 1,000 fields
- Resolved an error where workbooks for virtual source code modules could break trace execution when they contained no sheets
- Fixed arrow styling inconsistencies in the administration interface navigation

### Rule Services

- Fixed an `UnsupportedOperationException` when retrieving test results with the `unpaged=true` parameter
- Resolved deployment failures from database and folder repositories when permissions were configured at the project level

### Security

- Corrected error messages displayed during OAuth2 Identity Provider authentication failures to provide accurate diagnostic information
- Fixed a race condition in role saving where simultaneous permission updates could produce inconsistent results — roles are now saved sequentially with comprehensive error collection

## Technical Updates

- Upgraded Spring Framework to 6.2.15 and Spring Boot to 3.5.10, providing enhanced security, performance, and Jakarta EE 10 alignment
- Upgraded Spring Security to 6.5.7 with improved OAuth2, SAML, and ACL support
- Upgraded Hibernate ORM to 6.6.42 with Jakarta Persistence 3.0 for better query performance
- Upgraded Jetty to 12.1.6 with Jakarta EE 10 (ee10) modules
- Upgraded ASM to 9.9.1 and ByteBuddy to 1.18.4 for improved bytecode generation compatibility
- Upgraded HikariCP to 7.0.2 for enhanced database connection pooling
- Upgraded Jackson to 2.21.0 for improved JSON processing
- Upgraded Apache CXF to 4.1.4 for modernized web services support
- Upgraded gRPC to 1.79.0 for current protocol buffer support
- Added Spring AI 1.1.2 and MCP SDK 0.17.2 for AI assistant integration
- Added OpenTelemetry 2.24.0 Java agent for built-in observability
- Updated Node.js to v24.13.0 and npm to 11.7.0 for frontend builds
- Updated Ant Design to version 6 for the React-based administration interface
- Migrated Docker base images to Eclipse Temurin 25 JRE on Alpine Linux 3.23
- Upgraded TestContainers to 2.0.3 and JUnit to 6.0.2 for modernized testing infrastructure

## Breaking Changes

### Java 21 Minimum Requirement

**What Changed:** The minimum supported Java version has been raised from Java 11 to Java 21. All OpenL Tablets components now require Java 21 or higher to compile and run.

**Impact:** All deployments must upgrade their Java runtime to version 21 or higher. Docker images ship with Java 25 JRE by default. Custom build scripts, CI/CD pipelines, and deployment configurations need to reference Java 21+.

**Migration:** Update your `JAVA_HOME` to point to a Java 21+ distribution (Eclipse Temurin recommended). If using Docker, the official images already include the correct Java version. For WAR deployments, ensure your application server runs on Java 21+.

### Jakarta EE 10 Namespace Migration

**What Changed:** All Java EE APIs have been migrated from the `javax.*` namespace to `jakarta.*`. This affects servlet, JPA, XML binding, mail, annotation, and WebSocket APIs throughout the entire codebase.

**Impact:** Any custom code, plugins, or extensions that reference `javax.servlet`, `javax.persistence`, `javax.ws.rs`, `javax.mail`, or related packages must be updated to use the corresponding `jakarta.*` packages. Third-party libraries must also be compatible with Jakarta EE 10.

**Migration:**
1. Find and replace `javax.servlet` with `jakarta.servlet`, `javax.persistence` with `jakarta.persistence`, and similar for all EE packages
2. Update third-party dependencies to their Jakarta-compatible versions
3. Recompile all custom code against the new APIs
4. Note: `javax.crypto`, `javax.naming`, and `javax.xml.parsers` are **not** part of Jakarta EE and remain unchanged

### Spring Boot 3.x Configuration Changes

**What Changed:** Spring Boot has been upgraded from 2.x to 3.5.x. This changes auto-configuration class names, property keys, and default behaviors.

**Impact:** Custom application properties files, Spring configuration classes, and auto-configuration extensions may need updates. Actuator endpoints, security configurations, and data source properties may use different keys or defaults.

**Migration:** Review the [Spring Boot 3.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide) for detailed property changes. Test your custom configurations thoroughly in a staging environment before upgrading production.

### Simplified Instantiation Strategy API

**What Changed:** The multi-strategy instantiation system (`CommonRulesInstantiationStrategy`, `ApiBasedInstantiationStrategy`) has been consolidated. Custom instantiation strategies based on the old abstract classes are no longer supported.

**Impact:** Code that directly instantiates or extends `CommonRulesInstantiationStrategy` or `SimpleMultiModuleInstantiationStrategy` must be updated.

**Migration:** Use `RulesEngineFactory` for single-module projects and `SimpleProjectEngineFactory` for multi-module projects. Both provide a cleaner, builder-based API. See the updated API documentation for examples.

## Deprecations

### Legacy JSF Administration Interface

**Status:** Deprecated in 6.0.0, planned for removal in a future release

**Alternative:** Use the new React-based Admin Panel available at `/admin`. All administration functions — users, groups, security, repositories, system settings — are available in the new interface.

**Reason:** The JSF/RichFaces-based administration interface relies on a forked, end-of-life UI framework that is increasingly difficult to maintain and extend. The new React-based interface provides a faster, more responsive, and more accessible experience.

## Installation & Upgrade

### New Installations

For new installations, download OpenL Tablets 6.0.0 from our [downloads page](https://openl-tablets.org/downloads) and follow the [installation guide](https://openl-tablets.org/documentation/installation).

**Docker quick start:**
```
docker run -p 8080:8080 openltablets/webstudio:6.0.0
```

### Upgrading from Previous Versions

**From 5.27.x:**
- This is a major upgrade — review all Breaking Changes above carefully
- Upgrade your Java runtime to 21 or higher before deploying
- Update any custom code to use `jakarta.*` instead of `javax.*` packages
- Review Spring Boot 3.x property changes if you use custom configurations
- Backup your existing installation and database before upgrading
- Follow the [upgrade guide](https://openl-tablets.org/documentation/upgrade)

**From 5.26.x or earlier:**
- Additional migration steps may be required
- Review all release notes between your version and 6.0.0
- Consider upgrading to the latest 5.27.x first, then to 6.0.0
- Contact OpenL support for enterprise installations with complex configurations

### System Requirements

- Java 21 or higher (Java 25 recommended for Docker deployments)
- Minimum 4GB RAM (8GB recommended for production)
- Modern web browser for the Admin Panel (Chrome, Firefox, Edge, Safari)

## Resources

- **Documentation:** [https://openl-tablets.org/documentation](https://openl-tablets.org/documentation)
- **Download:** [https://openl-tablets.org/downloads](https://openl-tablets.org/downloads)
- **Release Notes Archive:** [https://openl-tablets.org/release-notes](https://openl-tablets.org/release-notes)
- **Community Forum:** [https://openl-tablets.org/community](https://openl-tablets.org/community)
- **GitHub:** [https://github.com/openl-tablets/openl-tablets](https://github.com/openl-tablets/openl-tablets)
- **Support:** [https://openl-tablets.org/support](https://openl-tablets.org/support)

# OpenL Tablets 6.0.0 Release Notes

**Release Date:** January 2026

OpenL Tablets 6.0.0 is a major platform modernization release that upgrades the foundation to Java 21 and Jakarta EE 10, redesigns administration with a modern React interface, and streamlines deployment through a new REST API. These changes position OpenL Tablets for long-term enterprise support while significantly improving the day-to-day experience for administrators and DevOps teams.

Administrators benefit from a faster, more intuitive management interface with real-time feedback. DevOps teams gain programmatic deployment control through a modern REST API. Developers and integrators should review the breaking changes section carefully, as this release removes several legacy features and requires Java 21 and Jakarta EE 10 namespaces.

> **Important**: This is a major release with breaking changes. Review the [Breaking Changes](#3-breaking-changes) section before upgrading.

## 1. New Features

### 1.1 Platform Modernization: Java 21 and Jakarta EE 10

OpenL Tablets now requires Java 21 as the minimum runtime and has fully migrated from Java EE (`javax.*`) to Jakarta EE 10 (`jakarta.*`). The platform has been validated for compatibility up to Java 25.

**Why it matters:** Java 21 is a Long-Term Support release with support through 2029, providing improved performance through virtual threads, better garbage collection, and access to the latest security patches. The Jakarta EE 10 migration aligns OpenL Tablets with the current enterprise Java ecosystem, enabling compatibility with Spring Framework 6.2, Spring Boot 3.5, and modern application servers.

**How it works:** All core components, bytecode generation, and runtime execution have been updated for Java 21. The ASM bytecode library has been upgraded to 9.9.1 for full Java 21+ class file format support. Spring Boot has been upgraded to 3.5.10 and Spring Framework to 6.2.15, both requiring Jakarta EE 10 namespaces. Security configuration has been migrated from XML-based to Java-based configuration following Spring Security 6.x best practices.

**Who it's for:** All users. Every deployment must upgrade to Java 21 and update any custom code using `javax.*` imports to `jakarta.*`.

**Notes:** See the [Breaking Changes](#3-breaking-changes) section for detailed migration steps.

### 1.2 Modern React-Based Administration Interface

The administration interface has been completely rebuilt with React, replacing the legacy JSF/RichFaces implementation. The new interface provides real-time WebSocket notifications, eliminating the previous polling-based approach.

**Why it matters:** Common administrative tasks that previously required multiple page navigations and manual refreshes can now be accomplished with fewer clicks and immediate visual feedback. Repository synchronization, deployment progress, and concurrent editing awareness are all delivered in real time.

**How it works:** The new interface covers all key administration areas: security settings (multi-user, Active Directory, SAML, OAuth2), user and group management, repository configuration with connection testing, and centralized system settings. The access control model has been simplified with consolidated permission types, project-level and repository-level ACL, and a new Import/Export API for backing up and migrating ACL configurations.

**Who it's for:** Administrators managing OpenL Studio instances, security configurations, and user access.

### 1.3 Deployment and API Modernization

A new Deployment REST API provides programmatic control over the entire deployment lifecycle, replacing the legacy Deploy Configuration feature. The Tables API has been enhanced with full pagination support for large projects.

**Why it matters:** DevOps teams can now integrate OpenL deployments directly into CI/CD pipelines using standard REST calls. Projects with hundreds of tables no longer need to load all definitions at once, improving both client performance and server resource utilization.

**How it works:** The Deployment API supports deploying projects, monitoring status via polling or WebSocket, rolling back versions, and querying deployment history. For interactive deployments, a new React modal dialog provides real-time progress display and validation feedback directly within Studio. The Tables API now accepts `page` and `size` query parameters and returns total element counts, with an unpaged mode for backwards compatibility. Selectors support case-insensitive partial match filtering for easier searching.

**Who it's for:** DevOps engineers, CI/CD pipeline authors, and developers integrating with OpenL Rule Services.

### 1.4 Additional Features

- **Simplified InstantiationStrategy architecture:** Merged `CommonRulesInstantiationStrategy` into `SimpleMultiModuleInstantiationStrategy` and consolidated method handlers, providing a cleaner API for multi-module projects.
- **Merge API with React UI:** Visual interface for managing branch merges and conflict resolution.
- **OAuth2/OIDC compliance:** User identity handling updated to use the `sub` claim per OIDC specification, improving interoperability with Keycloak, Okta, Azure AD, and other providers.
- **Academic citation support:** Added `CITATION.cff` for proper academic citation of OpenL Tablets.
- **Contribution guidelines:** Added `CONTRIBUTING.md` with project contribution guidelines and Code of Conduct.

## 2. Improvements

**Performance and Reliability**

- Core modules (grammars, core) merged into the rules module for simplified architecture and faster build times.
- Removed JGraphT dependency, replaced with a simpler built-in implementation.
- Removed Spring dependency from `org.openl.rules.project`, enabling lighter deployments.
- Correct classloader handling for Spring Context in complex deployment scenarios.

**User Experience**

- Project tags are now saved to files, surviving restarts and repository operations.
- User profiles display an administrator status indicator.
- Case-insensitive filtering in selectors improves search usability.
- ISO-8601 date formatting for Date cell values improves internationalization.

**Configuration and Administration**

- External scripts configuration included in settings responses for extensibility.
- Simplified OpenAPI schemas using `@Hidden` annotation for cleaner API documentation.
- Custom handlers can now access `ProjectDescriptor` (rules.xml) for advanced customization.

**Build and Infrastructure**

- EditorConfig standardization for consistent coding styles across the project.
- Dependabot configured for automated dependency updates across multiple package ecosystems.
- GitHub workflow for automated beta releases.
- Updated Docker images to use Alpine base image with reduced vulnerabilities.
- Podman support in rootless mode for containerized deployments.

**Security**

- Upgraded to Node.js 24.13.0 to address known vulnerabilities.
- Updated Docker images to use the latest Alpine base image.

## 3. Breaking Changes

### 3.1 Java 21 Minimum Requirement

- **Impact:** All deployments. Applications running on Java 11 or 17 will not start.
- **Migration:**
  1. Install JDK 21 or later (recommended: Eclipse Temurin or Amazon Corretto).
  2. Update `JAVA_HOME` environment variable.
  3. Update Docker base images and CI/CD pipelines to use Java 21.
  4. Rebuild any custom extensions with JDK 21.

### 3.2 Jakarta EE 10 Namespace Migration

- **Impact:** All custom extensions, integrations, and rule interceptors using Java EE APIs.
- **Migration:**
  1. Replace all `import javax.servlet.*` with `import jakarta.servlet.*` (and similarly for `persistence`, `validation`, `annotation` packages).
  2. Update XML namespaces in configuration files.
  3. Update Maven dependencies to Jakarta EE 10 versions.
  4. Rebuild and test thoroughly.

### 3.3 JSON Serialization Type Format Change

- **Impact:** Clients expecting CLASS-based type identifiers in JSON responses.
- **Migration:** Update client applications to handle NAME-based type identifiers. To temporarily retain CLASS format, set `ruleservice.jackson.jsonTypeInfoId=CLASS`.

### 3.4 Removed CAS SSO Support

- **Impact:** Organizations using CAS for single sign-on.
- **Migration:** Configure an OAuth2/OIDC provider (Keycloak, Okta, Azure AD) or expose an OAuth2/OIDC interface on the existing CAS server.

### 3.5 Removed Deploy Configuration Feature

- **Impact:** Deployments using the legacy Deploy Configuration UI or endpoints.
- **Migration:** Use the new Deployment REST API or the React deployment dialog for manual deployments.

### 3.6 Removed RMI Protocol and Variations Support

- **Impact:** Clients using RMI protocol or the variations feature.
- **Migration:** Migrate RMI clients to REST or gRPC. Convert variations to parameterized rules or separate rule versions.

### 3.7 Removed Cassandra and Hive Repository Support

- **Impact:** Deployments using Cassandra or Hive for repository storage.
- **Migration:** Export rules and import into a supported repository (Git, database, S3, or Azure Blob).

### 3.8 JDBC Drivers No Longer Bundled

- **Impact:** Deployments using database repositories.
- **Migration:** Download the JDBC driver for your database and place the JAR in the classpath (`lib/` directory or mounted Docker volume).

### 3.9 Removed Install Wizard, Demo User Mode, and Activity Logging

- **Impact:** Fresh installations, demo environments, and compliance workflows relying on built-in activity logging.
- **Migration:** Configure via `application.properties` or environment variables. Create test users through the administration interface. Use external logging solutions for audit requirements.

### 3.10 Removed Legacy APIs

- **Impact:** Integrations using deprecated ACL or repository endpoints.
- **Migration:**
  - `/rest/admin/management/groups/settings` → `/api/v1/admin/acl/*`
  - `/rest/admin/management/old/groups` → `/api/v1/admin/groups/*`
  - `/rest/repo/*` → `/api/v1/repositories/*`

## 4. Known Issues

- Windows 11 builds with webpack 6.0.0 require updated Node.js tooling.
- Groovy scripts may require adjustments for compatibility with updated dependencies.

---

## Bug Fixes

- Fixed `DoubleRange.contains()` to properly handle NaN values according to IEEE 754 semantics.
- Fixed method graph visualization for overloaded decision tables and multicall method dependencies.
- Fixed NullPointerException when using project dependencies with non-branch enabled database repositories.
- Fixed OAuth2 settings update requiring re-entry of Client Secret.
- Fixed adding the first repository in fresh installations.
- Fixed projects REST API request handling and remote repository URI configuration.
- Fixed repository page crash when `repository.name` is configured as read-only.
- Fixed unpredictable order of AccessDeniedHandler execution.
- Fixed building under Windows 11 and Java 25 DEMO package configuration.
- Fixed test compatibility with new AWS regions added in recent AWS SDK versions.

## Updated Libraries

| Library | Previous | New |
|---------|----------|-----|
| Spring Framework | 6.1.x | 6.2.15 |
| Spring Boot | 3.3.x | 3.5.10 |
| Spring Security | 6.3.x | 6.5.7 |
| Jackson | 2.17.x | 2.21.0 |
| Apache CXF | 4.0.x | 4.1.4 |
| gRPC | 1.65.x | 1.78.0 |
| Hibernate ORM | 6.5.x | 6.6.41.Final |
| ASM | 9.7 | 9.9.1 |
| Byte Buddy | 1.14.x | 1.18.4 |
| Jetty | 12.0.x | 12.1.5 |
| Apache POI | 5.2.x | 5.5.1 |
| OpenTelemetry | 2.5.x | 2.24.0 |
| Kafka | 3.7.x | 4.1.1 |
| Node.js | 22.x | 24.13.0 |
| AWS SDK | 2.26.x | 2.41.18 |
| Azure Storage Blob | 12.27.x | 12.33.1 |
| JUnit | 5.10.x | 6.0.2 |
| TestContainers | 1.19.x | 2.0.3 |
| Bouncy Castle | 1.78.x | 1.83 |
| Log4j | 2.23.x | 2.25.3 |

## Removed Features Summary

| Feature | Replacement |
|---------|-------------|
| CAS SSO Support | OAuth2/OIDC or SAML |
| Variations Support | Parameterized rules or separate versions |
| Deploy Configuration | Deployment REST API |
| Install Wizard | Configuration files or environment variables |
| RMI Protocol | REST or gRPC |
| Cassandra/Hive Repository | Git, database, S3, or Azure Blob |
| Embedded JDBC Drivers | Provide drivers externally |
| Legacy ACL/Repository APIs | New `/api/v1/` endpoints |
| Activity Logging | External logging solutions |
| Demo User Mode | Standard authentication |

---

**GitHub Release**: [v6.0.0 on GitHub](https://github.com/openl-tablets/openl-tablets/releases/tag/6.0.0)

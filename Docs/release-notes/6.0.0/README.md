## Release Notes

[v6.0.0](https://github.com/openl-tablets/openl-tablets/releases/tag/6.0.0) on the GitHub

OpenL Tablets 6.0.0 is a major release that modernizes the platform foundation and introduces AI-powered rule authoring capabilities. The entire administration interface has been rebuilt as a React single-page application, replacing the legacy JSF screens. The platform now requires Java 21 and has migrated from Java EE 8 to Jakarta EE 10, with Spring Boot upgraded to 3.5 and Spring Framework to 6.2.

Key highlights include a built-in MCP (Model Context Protocol) server enabling AI assistants to manage business rules, a simplified three-role access control model, Personal Access Tokens for API and CI/CD authentication, and a visual branch merge conflict resolution wizard.

## **New Features**

### **Redesigned Administration Panel**

The entire OpenL Studio administration interface has been rebuilt using React 19, TypeScript, and Ant Design 6. The new single-page application replaces the legacy JSF-based admin screens and provides a modern, responsive experience for managing all platform settings.

  * System configuration, security, and repository management
  * User and group management with real-time profile synchronization
  * Notification preferences, tag configuration, and email settings
  * Full internationalization with i18next and Zustand-based state management

![Image](images/admin-panel.png)

---

### **MCP Server for AI-Assisted Rule Authoring**

OpenL Studio now includes a built-in Model Context Protocol (MCP) server that enables AI assistants to interact with the platform programmatically. The MCP integration exposes a comprehensive set of tools for project and rule lifecycle management.

  * List, open, and inspect projects and repositories
  * Create, read, update, and append rule tables
  * Run tests with targeted table and range selection
  * Deploy and redeploy projects to production repositories
  * Browse project revision history and restore previous versions
  * Create branches and manage project status
  * Guided prompts for rule creation, test authoring, and deployment
  * OAuth 2.0 resource metadata discovery via `/.well-known/oauth-protected-resource`

---

### **Simplified Role-Based Access Control**

The access control model has been streamlined from a complex permission matrix to three clear roles: Viewer (read-only access), Contributor (read, create, edit, and delete), and Manager (full control including administration). Permissions can be assigned at the repository root level or on individual projects.

  * Three intuitive roles: Viewer, Contributor, Manager
  * Repository-level and project-level permission assignment
  * Visual permission editor with per-repository and per-project tabs
  * Default group display with informational tooltip on Users and Groups tabs

![Image](images/access-management.png)

---

### **Personal Access Tokens**

Users can now generate Personal Access Tokens (PATs) for authenticating REST API calls, CI/CD pipelines, and automated scripts without sharing their credentials. Tokens are created through the administration panel with configurable expiration periods (7, 30, 60, 90 days, custom date, or no expiration). The token value is displayed only once at creation time and authenticated via the `Authorization: Token <value>` header.

![Image](images/personal-access-tokens.png)

---

### **Branch Merge Conflict Resolution**

A new visual merge wizard guides users through branch synchronization with step-by-step conflict resolution. The merge modal allows selecting source and target branches, displays detected conflicts, and provides tools to resolve each conflicting file. Non-conflicting sheets are merged automatically, reducing manual resolution effort.

![Image](images/merge-conflict-resolution.png)

## **Improvements**

### **Platform Modernization**

  * Migrated from Java EE 8 to Jakarta EE 10 with Jakarta Servlet 6.0, Jakarta XML Bind 4.0, and Jakarta Mail 2.1
  * Upgraded to Java 21 as the minimum required JDK version
  * Upgraded to Spring Boot 3.5.10 and Spring Framework 6.2.15
  * Upgraded to Spring Security 6.5.7

---

### **OpenL Studio**

  * Added new REST API to retrieve the branch list for a project when the user lacks full repository permissions
  * Added real-time user profile synchronization when current user information is updated
  * Improved application redirect rules with a dedicated RedirectToRoot component and added test coverage
  * Added URL encoding for request URLs to prevent malformed path issues
  * Restricted group loading to administrators only for improved security and performance
  * Improved user form cleanup and reset behavior when closing drawers
  * Renamed the "Save User" button label for consistency with the UI conventions

---

### **Core Engine**

  * Simplified the rules instantiation API by consolidating CommonRulesInstantiationStrategy into SimpleMultiModuleInstantiationStrategy
  * Replaced InstantiationStrategy usage with RulesEngineFactory and SimpleProjectEngineFactory for cleaner project loading
  * Removed unreachable code paths and reduced internal API visibility for better encapsulation
  * Improved testing coverage for multi-module projects

## **Bug Fixes**

  * Fixed deployment failures from database and folder repositories when permissions are configured at the project level
  * Fixed trace execution breaking when a virtual source code module workbook had no sheets
  * Fixed UnsupportedOperationException when retrieving test results with the unpaged=true parameter
  * Fixed duplicate generic error messages appearing during user profile and form operations
  * Fixed arrow styling issues in the administration interface
  * Fixed race conditions in role saving by enforcing sequential save operations
  * Fixed validation messages and styles in user management forms
  * Fixed multiple CVE vulnerabilities through comprehensive dependency updates

## **Security & Library Updates**

### **Security Vulnerability Fixes**

  * Updated OpenSAML from 5.1.6 to 5.2.0 to address security vulnerabilities
  * Updated Jakarta Faces from 4.0.12 to 4.0.14 for security fixes
  * Updated Jetty from 12.1.5 to 12.1.6 for security patches
  * Replaced vulnerable lz4-java dependency with patched fork (at.yawk.lz4:lz4-java)
  * Updated frontend dependencies to address npm audit findings

---

### **Major Library Upgrades**

#### **Runtime Dependencies**

  * Spring Boot 3.5.10
  * Spring Framework 6.2.15
  * Spring Security 6.5.7
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
  * OpenSAML 5.2.0

#### **Test Dependencies**

  * JUnit 6.0.2
  * Mockito 5.21.0
  * Awaitility 4.3.0
  * Byte Buddy 1.18.4
  * Testcontainers 2.0.3
  * Testcontainers Keycloak 4.1.1
  * Minio 8.6.0

#### **Removed Dependencies**

  * Original lz4-java (org.lz4:lz4-java) replaced by at.yawk.lz4:lz4-java fork

## **Migration Notes**

### **Java 21 Required**

Java 21 is now the minimum required JDK version. Update your runtime environment before deploying 6.0.0.

---

### **Jakarta EE 10 Migration**

The platform has migrated from Java EE 8 to Jakarta EE 10. All `javax.*` namespace references in custom code or configuration must be updated to `jakarta.*`.

---

### **Spring Boot 3.5 Upgrade**

Spring Boot has been upgraded from 2.x to 3.5. Review custom Spring configurations for compatibility with Spring Boot 3.x and Spring Framework 6.x conventions.

---

### **Database Schema Changes**

The user database schema has been updated with a new `OpenL_PAT_Tokens` table for Personal Access Tokens. Back up the database before upgrading. Flyway migrations will apply the schema changes automatically.

---

### **Access Control Model Changes**

The access control model has been simplified to three roles (Viewer, Contributor, Manager). Existing permission assignments may need to be reviewed and remapped to the new role model.

---

### **Rules Instantiation API Changes**

The internal rules instantiation API has been refactored. `ApiBasedInstantiationStrategy` has been removed, and `CommonRulesInstantiationStrategy` has been merged into `SimpleMultiModuleInstantiationStrategy`. Projects using these internal classes directly should migrate to `RulesEngineFactory` or `SimpleProjectEngineFactory`.

---

### **Legacy Administration UI**

The legacy JSF administration pages are still available but are superseded by the new React-based administration interface. Plan to adopt the new UI as legacy screens will be removed in a future release.

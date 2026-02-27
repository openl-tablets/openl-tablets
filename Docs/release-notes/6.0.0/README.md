## Release Notes

[v6.0.0](https://github.com/openl-tablets/openl-tablets/releases/tag/6.0.0) on the GitHub

OpenL Tablets 6.0.0 is a major release that modernizes the entire platform foundation. The administration interface has been completely rebuilt as a React single-page application, replacing the legacy JSF screens. A built-in MCP server enables AI assistants to create, test, and deploy business rules. The access control model has been simplified to three intuitive roles, and Personal Access Tokens provide secure API authentication for CI/CD workflows.

Under the hood, the platform now requires Java 21 and has fully migrated from Java EE 8 to Jakarta EE 10, with Spring Boot upgraded to 3.5, Spring Framework to 6.2, and Jetty to 12. New built-in statistical functions bring variance, standard deviation, covariance, correlation, and linear regression capabilities directly into OpenL rules.

## **New Features**

### **Redesigned Administration Panel**

The entire OpenL Studio administration interface has been rebuilt using React 19, TypeScript 5.9, and Ant Design 6. The new single-page application replaces the legacy JSF-based admin screens and provides a modern, responsive experience for managing all platform settings.

  * System configuration with core engine settings, test threading, and history management
  * Security and authentication management supporting five modes: Single-User, Multi-User, Active Directory, SAML, and OAuth2/OIDC
  * User and group management with real-time profile synchronization via WebSocket
  * Repository management for Git, AWS S3, Azure Blob Storage, JDBC, JNDI, and local file system
  * Personal Access Token management with configurable expiration
  * Tag configuration, email server settings, and system-wide notification broadcast
  * Internationalization foundation with i18next and Zustand-based state management

![Image](images/admin-panel.png)

---

### **MCP Server for AI-Assisted Rule Authoring**

OpenL Studio now includes a built-in Model Context Protocol (MCP) server that enables AI assistants to interact with the platform programmatically. Built on Spring AI and the official MCP SDK, the server exposes 20 tools and 12 guided prompt templates covering the full project and rule lifecycle.

  * List, open, and inspect projects across design and deployment repositories
  * Create, read, update, and append rule tables including Decision Tables, Spreadsheets, and Datatypes
  * Run tests with targeted table and range selection, and analyze results
  * Deploy and redeploy projects to production repositories
  * Browse project revision history and restore previous versions
  * Create branches and manage project status
  * Guided prompt templates for rule creation, test authoring, error analysis, and deployment workflows
  * Authenticated via Personal Access Tokens or OAuth 2.0 Bearer Tokens
  * OAuth 2.0 resource metadata discovery via `/.well-known/oauth-protected-resource`

---

### **Simplified Role-Based Access Control**

The access control model has been streamlined from the complex ACL permission matrix introduced in 5.27.0 to three clear roles: Viewer (read-only access), Contributor (read, create, edit, and delete), and Manager (full control including administration). Permissions can be assigned at the repository root level or on individual projects.

  * Three intuitive roles: Viewer, Contributor, Manager
  * Repository-level and project-level permission assignment
  * Visual permission editor with per-repository and per-project tabs
  * Default group display with informational tooltip on Users and Groups tabs

![Image](images/access-management.png)

---

### **Personal Access Tokens**

Users can now generate Personal Access Tokens (PATs) for authenticating REST API calls, MCP connections, CI/CD pipelines, and automated scripts without sharing their credentials. Tokens are created through the administration panel with configurable expiration periods (7, 30, 60, 90 days, custom date, or no expiration). The token value is displayed only once at creation time and authenticated via the `Authorization: Token <value>` header.

  * Cryptographically secure token generation with BCrypt hashing
  * Available when OpenL Studio is configured with OAuth2 or SAML authentication
  * Full REST API for token lifecycle management (create, list, delete)

![Image](images/personal-access-tokens.png)

---

### **Branch Merge Conflict Resolution Wizard**

A new visual merge wizard guides users through branch synchronization with step-by-step conflict resolution. The wizard supports two-directional merging (receive changes from or send changes to another branch) and provides four resolution strategies per conflicting file.

  * Automatic merge status checking when selecting a target branch
  * Four resolution strategies per file: keep ours, accept theirs, use base version, or upload a custom merge
  * Automatic sheet-level merging for non-conflicting Excel sheets, reducing manual effort
  * Download any revision (ours, theirs, base) for offline comparison
  * Branch protection awareness with visual warnings
  * Customizable merge commit message

![Image](images/merge-conflict-resolution.png)

---

### **New Statistical Functions**

A comprehensive suite of statistical functions is now available as built-in functions in OpenL rules, providing Excel-compatible analytics without external libraries. All functions support multiple numeric types (Double, Float, BigDecimal, BigInteger) and handle null values gracefully.

  * **Variance**: `varS()` (sample) and `varP()` (population) for calculating data dispersion
  * **Standard Deviation**: `stdevS()` (sample) and `stdevP()` (population) as the square root of variance
  * **Covariance**: `covarS()` (sample) and `covarP()` (population) for measuring how two datasets vary together
  * **Correlation**: `correl()` for Pearson correlation coefficient and `rsq()` for R-squared (coefficient of determination)
  * **Linear Regression**: `slope()` and `intercept()` for regression line parameters, and `forecast()` for predicting values using linear regression

## **Improvements**

### **Platform Modernization**

  * Migrated from Java EE 8 to Jakarta EE 10 with Jakarta Servlet 6.0, Jakarta XML Bind 4.0, Jakarta Mail 2.1, Jakarta CDI 4.0, and Jakarta WS-RS 4.0
  * Upgraded to Java 21 as the minimum required JDK version
  * Upgraded to Spring Boot 3.5.10 and Spring Framework 6.2.15
  * Upgraded to Spring Security 6.5.7
  * Upgraded to Jetty 12.1.6 with EE10 support

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

The platform has migrated from Java EE 8 to Jakarta EE 10. All `javax.*` namespace references in custom code or configuration must be updated to `jakarta.*`. This affects Servlet, XML Bind, Mail, CDI, WS-RS, and related APIs.

---

### **Spring Boot 3.5 Upgrade**

Spring Boot has been upgraded from 2.x to 3.5. Review custom Spring configurations for compatibility with Spring Boot 3.x and Spring Framework 6.x conventions.

---

### **Database Schema Changes**

The user database schema has been updated with a new `OpenL_PAT_Tokens` table for Personal Access Tokens. Back up the database before upgrading. Flyway migrations will apply the schema changes automatically.

---

### **Access Control Model Changes**

The access control model has been simplified from the fine-grained ACL system introduced in 5.27.0 to three roles (Viewer, Contributor, Manager). Existing permission assignments may need to be reviewed and remapped to the new role model.

---

### **Rules Instantiation API Changes**

The internal rules instantiation API has been refactored. `ApiBasedInstantiationStrategy` has been removed, and `CommonRulesInstantiationStrategy` has been merged into `SimpleMultiModuleInstantiationStrategy`. Projects using these internal classes directly should migrate to `RulesEngineFactory` or `SimpleProjectEngineFactory`.

---

### **Legacy Administration UI**

The legacy JSF administration pages are still available but are superseded by the new React-based administration interface. Plan to adopt the new UI as legacy screens will be removed in a future release.

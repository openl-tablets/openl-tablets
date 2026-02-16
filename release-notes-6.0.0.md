# **OpenL Tablets 6.0.0 Release Notes**

OpenL Tablets **6.0.0** is a major release introducing a completely redesigned administration interface built with React 19 and TypeScript, a simplified role-based access control system with three predefined roles, full migration to Jakarta EE 10 and Java 21, and MCP (Model Context Protocol) integration for AI-powered rule authoring workflows. The release also adds Personal Access Tokens for secure API authentication and extends the REST API with new endpoints for branch management, table operations, and test execution.

This release also includes breaking changes that require careful review before upgrading.

## **Contents**

* [New Features](#new-features)
* [Improvements](#improvements)
* [Breaking Changes](#breaking-changes)
* [Security & Library Updates](#security--library-updates)
* [Bug Fixes](#bug-fixes)
* [Migration Notes](#migration-notes)

## **New Features**

### **Redesigned Administration Interface**

OpenL Studio introduces a **completely new administration interface** built with modern web technologies. The new interface replaces the legacy JSF/RichFaces administration pages with a responsive, single-page application that provides a streamlined experience for system configuration and user management.

The new administration UI is built on **React 19**, **TypeScript 5.9**, and **Ant Design 6**, with **Zustand** for state management and **i18next** for full internationalization support. It communicates with the backend through REST APIs and supports real-time notifications via WebSocket (STOMP protocol).

#### **System Configuration**

The system settings page provides centralized control over core OpenL Studio behavior:

* Database connection configuration (URL, credentials, connection pool)
* Auto-compile and dispatching validation toggles
* Date and time format patterns
* Test thread count configuration
* Project history management with the ability to clear all history
* Restore default settings with confirmation dialog

#### **Security Mode Management**

A dedicated security configuration page allows administrators to switch between authentication modes without editing property files:

* **Single-user mode** for development and evaluation
* **Multi-user mode** with internal user management
* **Active Directory** (LDAP) integration
* **SAML SSO** for enterprise single sign-on
* **OAuth2 SSO** for modern identity providers

Each mode presents its own configuration form with relevant fields and validation.

#### **User and Group Management**

The user management interface provides a complete set of administration tools:

* User table with admin indicator badges and email verification status
* Create, edit, and delete users with access rights management
* Group management with member counts and role assignments
* Per-project and per-repository access control assignments
* External group discovery for LDAP and SSO configurations
* Conditional display that hides internal user management when using external authentication

#### **Repository Management**

A dual-tab repository management page covers both design and deployment repositories:

* Support for Git, File System, Database, AWS S3, and Azure Blob storage backends
* Git-specific configuration: branch settings, SSH/HTTPS, clone URLs
* Database repository configuration with JDBC connection settings
* Cloud storage credential management for AWS S3 and Azure Blob
* Per-repository comment template configuration

#### **Additional Administration Features**

* **Tag Management**: Create and manage custom tag types and values with auto-save, configure project name templates for automatic tag extraction
* **Email Configuration**: SMTP server setup with connection validation and email verification support
* **Notification System**: Broadcast notifications to all active users with real-time delivery via WebSocket
* **User Profile**: Self-service profile editing with display name customization and password change
* **User Settings**: IDE preferences including table display options, test configuration, and trace settings

#### **Legacy UI Compatibility**

The legacy JSF pages remain available under `/faces/*` for backward compatibility. The new React administration interface coexists with the legacy UI, and both share the same backend APIs and authentication system.

---

### **Simplified User Access & Permissions Management**

OpenL Studio introduces a **completely redesigned access control system** based on Spring Security ACL. The new system replaces the previous fine-grained permission model with a simplified, role-based approach that reduces configuration complexity while maintaining enterprise-grade security.

The new system supports **granular, resource-level access control** through role assignments at the repository, project, and deployment target levels.

#### **Role-Based Access Control**

Access is now managed through three predefined roles:

* **Manager**: Full administrative control including the ability to assign roles to other users. Grants Administration, Read, Create, Write, and Delete permissions.
* **Contributor**: Content modification without system administration. Grants Read, Create, Write, and Delete permissions.
* **Viewer**: Read-only access with the ability to run tests. Grants Read permission only.

#### **Access Control API**

A comprehensive REST API manages access control:

* `GET /acls/roles` returns the list of available roles
* `GET /acls/projects` and `GET /acls/repositories` retrieve access rules by criteria
* `PUT` and `DELETE` endpoints for project-level and repository-level ACL management
* Bulk ACL configuration through `POST /acls`
* Repository root-level ACL management for default access settings

#### **Access Management UI**

The React administration interface includes dedicated access management components:

* **Design Repositories Tab**: Assign roles to users and groups at the design repository level
* **Deploy Repositories Tab**: Configure access for deployment targets
* **Projects Tab**: Per-project role assignments with multi-select support
* Dynamic role selection with "None" option for removing access

#### **Migration Notes**

1. ACL structures are migrated automatically during upgrade
2. The legacy permission model is replaced by the three-role system
3. Legacy "View Projects" permission maps to the Viewer role at repository level
4. Legacy "Edit Projects" permission maps to the Contributor role at repository level
5. Review role assignments and default group permissions after upgrade

---

### **Jakarta EE 10 and Java 21 Migration**

OpenL Tablets 6.0.0 completes the migration from **Java EE (javax)** to **Jakarta EE 10 (jakarta)**, and requires **Java 21** as the minimum runtime version.

#### **Namespace Migration**

All Java EE namespace references have been updated:

* `javax.servlet` replaced with `jakarta.servlet`
* `javax.persistence` replaced with `jakarta.persistence`
* `javax.ws.rs` replaced with `jakarta.ws.rs`
* `javax.mail` replaced with `jakarta.mail`
* `javax.xml.bind` replaced with `jakarta.xml.bind`
* `javax.annotation` replaced with `jakarta.annotation`

Standard Java SE packages (`javax.crypto`, `javax.naming`, `javax.xml.parsers`) remain unchanged as they are not part of Jakarta EE.

#### **Framework Upgrades**

The Jakarta EE migration drives major framework upgrades:

* Spring Framework 6.2.15 (from 5.x)
* Spring Boot 3.5.10 (from 2.x)
* Spring Security 6.5.7 (from 5.x)
* Hibernate ORM 6.6.42.Final (from 5.x)
* Jetty 12.1.6 (from 9.x/11.x)
* Jakarta Faces 4.0.14 (from javax.faces 2.x)

#### **Java 21 Features**

The codebase takes advantage of Java 21 capabilities:

* Record patterns and pattern matching
* Virtual threads compatibility
* `-parameters` compiler flag enabled for Spring Validation support

---

### **MCP (Model Context Protocol) Integration**

OpenL Tablets now includes a **fully-functional MCP server** that enables AI assistants and large language models to interact with the business rules engine programmatically. The MCP integration uses **Spring AI 1.1.2** with **MCP SDK 0.17.2** and supports tool invocation, structured prompts, and project management workflows.

#### **MCP Server Architecture**

The MCP server operates as a WebMvc-based stateless server transport at the `/mcp` endpoint. It uses Jackson-based JSON serialization and preserves the Spring Security context across tool invocations through immediate execution mode.

Configuration is annotation-driven: beans annotated with `@McpController` have their `@Tool` methods automatically discovered and registered as MCP tools.

#### **Project Management Tools**

The Projects MCP Controller provides comprehensive project lifecycle management:

* `openl_list_projects` queries projects with filters for status, repository, name, and tags
* `openl_get_project` returns full project details including modules, dependencies, and metadata
* `openl_update_project_status` opens, closes, saves, or switches branches with safety checks
* `openl_create_project_branch` creates new branches from specified revisions
* `openl_list_project_local_changes` and `openl_restore_project_local_change` manage local history

#### **Table Operation Tools**

AI assistants can create and modify business rules directly:

* `openl_list_project_tables` lists all tables with optional filtering and pagination
* `openl_get_project_table` returns detailed table structure including conditions, actions, and row data
* `openl_create_project_table` creates new tables in specified modules and sheets
* `openl_update_project_table` replaces full table content with modified structure
* `openl_append_project_table` adds new rows or fields without replacing existing data

#### **Test Execution Tools**

* `openl_run_project_tests` runs tests with options for targeting specific tables and test ranges, returning execution result summaries

#### **Repository and Deployment Tools**

* `openl_list_design_repositories` discovers available design repositories
* `openl_list_design_repository_branches` lists branches for branching-capable repositories
* `openl_list_design_repository_features` checks repository capabilities
* `openl_list_design_repository_project_revisions` retrieves commit history with pagination
* `openl_list_deployments` lists production deployments
* `openl_deploy_project` and `openl_redeploy_project` manage deployment operations

#### **Prompt Templates**

The MCP server includes **12 structured prompt templates** that guide AI assistants through complex workflows:

* **create_rule** covers decision tables (Rules, SimpleRules, SmartRules, SimpleLookup, SmartLookup), spreadsheets, and other table types
* **datatype_vocabulary** guides datatype and vocabulary/enum creation
* **create_test** and **update_test** provide step-by-step test table management
* **append_table** explains efficient data appending patterns
* **run_test** implements test scope selection logic
* **execute_rule** covers test data construction and rule execution
* **deploy_project** provides deployment workflow with validation and environment progression
* **get_project_errors** offers error analysis patterns and recommendations
* **dimension_properties** explains business versioning with dimension properties
* **file_history** and **project_history** guide Git-based version history exploration

---

### **Personal Access Tokens**

OpenL Tablets now supports **Personal Access Tokens (PATs)** for secure, programmatic API authentication. PATs provide an alternative to session-based authentication for automated workflows, CI/CD pipelines, and MCP client connections.

#### **Token Management**

Users can manage their tokens through the administration interface at `/administration/user/tokens`:

* Create tokens with custom names and configurable expiration (7, 30, 60, or 90 days, custom date, or no expiration)
* View all tokens with creation and expiration dates
* Delete tokens with confirmation
* Expired token visual indicators

The token value is displayed only once immediately after creation and cannot be recovered.

#### **Authentication**

PATs authenticate API requests using the HTTP Authorization header. Tokens use a base62-encoded public identifier (16 characters) and a secret hash for secure validation without storing the actual secret.

**Configuration:** PATs are enabled automatically for OAuth2 and SAML authentication modes where session-based browser authentication is not practical for API access.

---

### **Additional Features**

#### **Extended REST API**

The Projects API (BETA) introduces several new endpoints:

* `GET /projects/{projectId}/branches` returns available branches for a project, enabling users without full repository permissions to discover branches
* `POST /projects/{projectId}/branches` creates new branches within a project
* `POST /projects/{projectId}/tables` creates new tables in a project
* `PUT /projects/{projectId}/tables/{tableId}` updates existing tables
* `POST /projects/{projectId}/tables/{tableId}/lines` appends rows to existing tables
* `POST /projects/{projectId}/tests/run` executes tests at project or table level
* `GET /projects/{projectId}/tests/summary` retrieves test execution summaries in JSON or XLSX format

Table listing now supports Rules, Data, Test, SmartLookup, and SimpleLookup table types with name filtering and pagination.

#### **Project Tags in Repository**

Project tags are now stored inside the project structure as `tags.properties` files rather than in the database. This change makes tags version-controlled and portable across environments. Tags are migrated automatically from the database during upgrade.

## **Improvements**

### **API and Integration**

* Added name filtering, pagination with total count attributes, and unpaged option to the Get Projects and Get Tables APIs
* Added new REST API for running tests in OpenL Studio, enabling programmatic test execution and automation
* Implemented synchronous role saving to ensure role assignments are immediately persisted

### **User Interface**

* Improved user form interface with cleaned up layout and updated UI library versions
* Eliminated duplicate generic messages in the UI for improved user feedback consistency
* Improved button labels for clarity in user management forms
* Updated user profile to reflect changes immediately when current user info is modified
* Enhanced default group display with tooltip on Users and Groups tabs
* Limited group loading to admin-only viewing for better performance

### **Code Quality and Internals**

* Refactored the instantiation strategy framework, merging `CommonRulesInstantiationStrategy` into `SimpleMultiModuleInstantiationStrategy` and simplifying factory usage patterns
* Replaced `InstantiationStrategy` usage with `RulesEngineFactory` and `SimpleMultiModuleInstantiationStrategy` usage with `SimpleProjectEngineFactory`
* Removed unreachable code and reduced visibility of internal methods across the instantiation layer
* Improved testing of multi-module projects with better test isolation and verification

### **Infrastructure**

* Pinned npm version for `frontend-maven-plugin` to prevent spurious `package-lock.json` changes
* Updated Docker base image to `eclipse-temurin:21-jre-alpine`
* Added `compose.override` support for local Docker configuration customization

## **Breaking Changes**

### **Java 21 Requirement (CRITICAL)**

OpenL Tablets 6.0.0 requires **Java 21** or later. Earlier Java versions are no longer supported.

#### **What Changed**

* Minimum Java version raised from Java 11 to Java 21
* Maven compiler plugin configured with `<release>21</release>`
* Docker images based on `eclipse-temurin:21-jre-alpine`

#### **Impact**

* All runtime environments must be upgraded to Java 21 before deploying OpenL Tablets 6.0.0
* Custom Java code compiled against earlier Java versions may need recompilation

#### **Who Is Affected**

All users upgrading from any previous version of OpenL Tablets.

#### **Migration Steps**

1. Install Java 21 JDK or JRE on all servers running OpenL Studio or Rule Services
2. Update `JAVA_HOME` environment variable to point to Java 21
3. Recompile any custom Java extensions against Java 21

---

### **Jakarta EE Namespace Migration (CRITICAL)**

All Java EE namespace references (`javax.*`) have been replaced with Jakarta EE 10 namespace references (`jakarta.*`).

#### **What Changed**

* `javax.servlet.*` replaced with `jakarta.servlet.*`
* `javax.persistence.*` replaced with `jakarta.persistence.*`
* `javax.ws.rs.*` replaced with `jakarta.ws.rs.*`
* `javax.mail.*` replaced with `jakarta.mail.*`
* `javax.xml.bind.*` replaced with `jakarta.xml.bind.*`
* `javax.annotation.*` replaced with `jakarta.annotation.*`

Major framework upgrades:

* Spring Framework 6.2.15 (from 5.x)
* Spring Boot 3.5.10 (from 2.x)
* Spring Security 6.5.7 (from 5.x)
* Hibernate ORM 6.6.42.Final (from 5.x)
* Jetty 12.1.6 (from 9.x/11.x)

#### **Impact**

* Any custom Java code using `javax.*` packages for Servlets, JPA, JAX-RS, or JAXB must be updated to use `jakarta.*`
* Third-party libraries that depend on `javax.*` may need to be upgraded to Jakarta-compatible versions
* Custom Hibernate queries may need updates due to Hibernate 6 changes

#### **Who Is Affected**

You are affected only if your OpenL projects include:

* Custom Java code with `javax.*` imports for Java EE APIs
* Custom Servlet filters or listeners
* Custom JPA entities or repositories
* Direct Hibernate usage
* Third-party libraries not yet migrated to Jakarta EE

#### **Migration Steps**

If you are using OpenL Studio without custom Java extensions, no action is required. The internal migration is handled automatically.

The steps below apply only if you have custom Java code:

##### **Required namespace changes**

1. Replace all `javax.servlet` imports with `jakarta.servlet`
2. Replace all `javax.persistence` imports with `jakarta.persistence`
3. Replace all `javax.ws.rs` imports with `jakarta.ws.rs`
4. Replace all `javax.xml.bind` imports with `jakarta.xml.bind`
5. Replace all `javax.annotation` imports with `jakarta.annotation`

```java
// Before
import javax.persistence.Entity;
import javax.servlet.http.HttpServletRequest;

// After
import jakarta.persistence.Entity;
import jakarta.servlet.http.HttpServletRequest;
```

6. Update Maven dependencies to Jakarta-compatible versions
7. Recompile and test all custom extensions

---

### **Permission Model Redesign**

The legacy fine-grained permission model has been replaced with a simplified three-role access control system.

#### **What Changed**

* The previous permission model with granular individual permissions has been replaced
* Three predefined roles now govern all access: Manager, Contributor, and Viewer
* `security.default-group` no longer grants implicit READ access to all repositories
* Project creation and deletion are additionally controlled by the `security.allow-project-create-delete` system property

#### **Impact**

* Effective access permissions may differ after upgrade
* Users and groups assigned to the default group may lose access to projects they previously had implicit READ access to
* Role assignments may need manual review

#### **Who Is Affected**

All users upgrading from previous versions that used the legacy permission system with custom group or permission configurations.

#### **Migration Steps**

1. ACL structures are migrated automatically during upgrade
2. Review role assignments after upgrade to ensure users have appropriate access
3. Check default group configuration for unintended access changes
4. Review the `security.allow-project-create-delete` property setting

---

### **Configuration Property Changes**

Several configuration properties have been removed or renamed in OpenL Tablets 6.0.0.

#### **What Changed**

* **Removed**: `repository.{id}.folder-structure.flat` property (no longer needed)
* **Renamed**: `repository.{id}.local-repository-path` replaced by `repository.{id}.uri`
* **Removed**: `openl-projects.yaml` files from the locks root folder

These properties are migrated automatically during startup.

#### **Impact**

* Custom scripts or configuration management tools referencing removed properties must be updated
* Folder structure is now determined automatically

#### **Who Is Affected**

Users with custom configuration management or scripts that set these properties directly.

#### **Migration Steps**

1. Property migration is automatic during first startup after upgrade
2. Update any external configuration management tools to use the new property names
3. Replace references to `repository.{id}.local-repository-path` with `repository.{id}.uri`

---

### **Removed Features Summary**

The following features, protocols, and APIs have been completely removed from OpenL Tablets 6.0.0:

#### **Removed Configuration Properties**

* **`repository.{id}.folder-structure.flat`**
  * **Migration**: Removed automatically; folder structure is now determined internally
* **`repository.{id}.local-repository-path`**
  * **Migration**: Automatically converted to `repository.{id}.uri` during startup

#### **Removed Files**

* **`openl-projects.yaml` in locks directory**
  * **Migration**: Automatically removed; project index is managed through repository settings

## **Security & Library Updates**

### **Security Vulnerability Fixes**

* **CVE-2025-41249**: Fixed Spring Framework vulnerability (upgraded to 6.2.15)
* **CVE-2025-41248**: Fixed Spring Security vulnerability (upgraded to 6.5.7)
* Resolved Jackson deserialization vulnerability by using NAME-based type identification instead of CLASS-based by default

---

### **Major Library Upgrades**

#### **Runtime Dependencies**

* Spring Framework 6.2.15 (from 5.x)
* Spring Boot 3.5.10 (from 2.x)
* Spring Security 6.5.7 (from 5.x)
* Spring Integration 6.5.6
* Spring AI 1.1.2
* Hibernate ORM 6.6.42.Final (from 5.x)
* Hibernate Validator 8.0.3.Final
* Jackson 2.21.0
* Apache CXF 4.1.4
* Apache POI 5.5.1
* ASM 9.9.1
* Jetty 12.1.6
* Netty 4.2.10.Final
* Log4j 2.25.3
* SLF4J 2.0.17
* Swagger Core (OpenAPI) 2.2.42
* Guava 33.5.0-jre
* Groovy 4.0.30
* Gson 2.13.2
* BouncyCastle 1.83
* OpenSAML 5.2.0
* HikariCP 7.0.2
* Apache Commons Lang3 3.20.0
* Apache Commons IO 2.21.0
* Apache Commons Codec 1.21.0
* Apache Commons Compress 1.28.0
* AWS SDK 2.41.24
* Azure Storage 12.33.2
* Apache Kafka 4.1.1
* gRPC 1.79.0
* OpenTelemetry Instrumentation 2.24.0
* JAXB Runtime 4.0.6
* H2 Database 2.4.240
* Flyway 4.2.0.3

#### **Frontend Dependencies**

* Node.js v24.13.0
* npm 11.7.0
* React 19.x
* TypeScript 5.9.x
* Ant Design 6.x
* i18next 25.x
* Zustand 5

#### **Test Dependencies**

* JUnit 6.0.2
* Mockito 5.21.0
* TestContainers 2.0.3
* TestContainers Keycloak 4.1.1
* XMLUnit 2.11.0
* PostgreSQL Driver 42.7.9
* MariaDB Driver 2.7.13
* MSSQL Driver 13.2.1.jre11
* Oracle JDBC 23.26.1.0.0

#### **Jakarta EE APIs**

* Jakarta Servlet API 6.0.0
* Jakarta Annotation API 3.0.0
* Jakarta Mail API 2.1.5
* Jakarta WS.RS API 4.0.0
* Jakarta XML Bind API 4.0.5
* Jakarta EL API 5.0.1
* Jakarta Faces 4.0.14

## **Bug Fixes**

* Fixed `UnsupportedOperationException` when retrieving test results with the `unpaged=true` parameter
* Fixed deployment from database and folder repositories when permissions are configured at the project level
* Fixed trace execution failing when a workbook for a virtual source code module had no sheets
* Fixed URL encoding issues in request URLs, particularly in group management
* Fixed styling issues with arrow components in the UI
* Fixed validation messages and related styling issues
* Fixed application redirect logic with improved routing rules
* Fixed Git LFS not working with Bitbucket repositories
* Fixed OpenL Studio working incorrectly when a project with an OpenAPI file had a dependency on other projects
* Fixed Internal Server Error when deploying a project without specifying `base.path` in the configuration
* Fixed an issue where defining a Default Group would grant READ permission to all projects
* Fixed an `IllegalStateException` in Trace when a rule had a version with the `Origin='Deviation'` property
* Fixed dependency graph display for tables containing business dimensional properties
* Fixed table connection display on the dependencies graph when one table calls another using Multi Call
* Fixed OAuth2 not working with the default configuration

## **Migration Notes**

### **Quick Role-Based Pointers**

* **If you are a Rules Author** pay special attention to sections on **Jakarta EE Namespace Migration** and **Permission Model Redesign**
* **If you are a Developer** pay special attention to sections on **Java 21 Requirement**, **Jakarta EE Namespace Migration**, and **Configuration Property Changes**
* **If you are an Administrator / Platform Owner** pay special attention to sections on **Java 21 Requirement**, **Permission Model Redesign**, and **Redesigned Administration Interface**

---

### **Runtime Environment**

* Java **21** is required. Earlier Java versions are no longer supported.
* All `javax.*` packages have been replaced with `jakarta.*`. Custom Java code must be updated accordingly.
* Docker images are now based on `eclipse-temurin:21-jre-alpine`.

---

### **Access Control & Permissions**

* The legacy permission model has been replaced with role-based access control (RBAC).
  Permissions are migrated automatically, but effective access may differ after upgrade.
* `security.default-group` no longer grants implicit READ access to repositories.
* Project creation and deletion are additionally controlled by the system property:

```properties
security.allow-project-create-delete=true
```

* Role assignments and default group permissions should be reviewed after upgrade.

#### **Permission Mapping**

| Legacy Permission | New Behavior | Notes / Action |
| :---- | :---- | :---- |
| **View Projects** | Included in all roles | Deprecated; project viewing available to all roles |
| **Edit Projects** | Included in Contributor role | Covered by the Edit permission |
| **Admin** | Manager role | Full control including role assignments |

---

### **Configuration Properties**

* `repository.{id}.folder-structure.flat` has been removed (migrated automatically)
* `repository.{id}.local-repository-path` is replaced by `repository.{id}.uri` (migrated automatically)
* `openl-projects.yaml` files in the locks directory are removed automatically
* Update any external configuration management tools to reference new property names

---

### **Project Tags**

* Project tags are now stored inside the project structure as `tags.properties` files
* Tags are migrated automatically from the database during upgrade
* Tag changes are now version-controlled through the repository

---

### **Administration UI**

* The Administration UI has been fully redesigned with React
* No functional loss; all previously available settings remain accessible
* Familiarize yourself with the new layout at `/administration`
* Legacy JSF pages remain available at `/faces/*` for backward compatibility

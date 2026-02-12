# **OpenL Tablets 6.0.0 Release Notes**

OpenL Tablets **6.0.0** is a major release introducing a completely redesigned administration interface built with modern React technology, a simplified role-based access control system, full migration to Jakarta EE 10 and Java 21, and MCP (Model Context Protocol) integration for AI-powered workflows.

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

The administration panel has been completely redesigned using modern React technology, replacing the legacy JSF/RichFaces implementation. The new interface provides a more intuitive, responsive, and streamlined experience for system administrators managing OpenL Studio.

All administrative operations are now backed by RESTful APIs, enabling automation and integration scenarios beyond the UI.

#### **Security Settings**

Centralized management of authentication modes with support for:

* **Multi-user** mode with internal user database
* **Active Directory** integration
* **SAML** single sign-on
* **OAuth2** authentication providers

#### **User & Group Management**

* Create, edit, and delete users with an enhanced workflow
* Group administration with member count display and streamlined role assignment
* Dedicated **Access Rights** tab with an "All" dropdown for assigning default roles across repository roots

#### **Repository Management**

* Intuitive configuration for **Git**, **AWS S3**, and **JDBC** repositories
* Built-in connection testing to verify repository accessibility
* Support for both Design and Deployment repository types

#### **System Settings**

Centralized management of system properties including:

* Date format configuration
* Auto-compile settings
* Thread pool configurations
* WebSocket-based messaging between the admin panel and connected users for real-time notifications

---

### **Simplified User Access & Permissions Management**

OpenL Studio introduces a **completely redesigned access control system** with a simplified, role-based permission model. The new approach replaces the previous 16-permission model with a streamlined 3-role system, significantly reducing configuration complexity while maintaining enterprise-grade security.

The new system supports **granular, resource-level access control** through role assignments, with permissions applied at the repository level. Administrators can grant specific roles for Design and Deployment repositories independently.

#### **Role-Based Access Control**

Access is now managed through three predefined roles:

* **Manager**: Full administrative control including the ability to assign access rights to other users
* **Contributor**: Full edit access including creating, modifying, and deleting content
* **Viewer**: Read-only access to resources with test execution capabilities

#### **External Identity Provider Integration**

The ACL system integrates with external user management systems:

* Active Directory
* SAML providers
* OAuth2 providers

#### **Batch API for ACL Management**

A new REST API enables bulk import and export of ACL configurations, facilitating automated deployment processes across multiple environments. This is particularly useful for organizations managing permissions across development, staging, and production instances.

#### **Migration Notes**

1. ACL structures are migrated automatically during upgrade
2. Legacy permissions are automatically mapped to new roles:
   * Legacy "View Projects" → Viewer role at repository level
   * Legacy "Edit Projects" → Contributor role at repository level
   * Legacy administrative permissions → Manager role
3. Review effective permissions after upgrade, as the simplified model may change access boundaries

---

### **Jakarta EE 10 and Java 21 Migration**

OpenL Tablets 6.0.0 completes the migration to Jakarta EE 10 and Java 21, providing a modern, secure, and high-performance foundation for the platform. This migration affects both OpenL Studio and Rule Services.

The migration removes reflection hacks previously needed for Java compatibility, improving stability and maintainability across the platform.

#### **Major Framework Upgrades**

* Spring Framework 6.2 (from 5.x)
* Spring Security 6.4 (from 5.x)
* Spring Boot 3.4 (from 2.x)
* OpenSAML 5.1 (from 4.x)
* Hibernate ORM 6.6 (from 5.x)
* Hibernate Validator 8.0 (from 7.x)
* Jetty 12 (from 11.x)

#### **Namespace Changes**

All `javax.*` EE API packages have been replaced with `jakarta.*`:

```java
// Before (5.x)
import javax.servlet.http.HttpServletRequest;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

// After (6.0.0)
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
```

#### **Docker Image Updates**

Docker images have been updated to use `eclipse-temurin:21-jre-alpine` as the base image, which typically contains no known vulnerabilities.

#### **Migration Notes**

1. Java 21 is now required; earlier Java versions are no longer supported
2. All custom Java code using `javax.*` EE packages must be updated to `jakarta.*`
3. Review and update any custom filters, listeners, or interceptors
4. Update deployment descriptors (`web.xml`) if using `javax` references

---

### **MCP (Model Context Protocol) Integration**

OpenL Tablets 6.0.0 introduces support for the Model Context Protocol (MCP), enabling integration with AI assistants such as Claude Desktop, ChatGPT Desktop, and IDE plugins. This unlocks new possibilities for AI-powered business rule management and interaction.

#### **OAuth 2.1 Discovery**

The implementation includes an OAuth 2.1-compliant `/.well-known/oauth-protected-resource` endpoint that allows MCP clients to dynamically discover OAuth configuration and initiate Authorization Code + PKCE login flows.

#### **AI-Powered Workflows**

A comprehensive REST API has been extended to support AI-powered workflows:

* Table viewing, editing, and creation through the API
* Pagination and filtering support for projects and tables
* Test execution through REST endpoints

#### **Deployment**

The MCP server has been moved to a dedicated repository for independent development and deployment, allowing faster iteration cycles separate from the main OpenL Tablets release schedule.

---

### **Additional Features**

#### **Personal Access Tokens (PATs)**

OpenL Studio now supports Personal Access Tokens for secure, non-interactive authentication. PATs are intended for CI/CD pipelines, service-to-service communication, and automation scripts.

##### **Key characteristics:**

* Optional expiration dates for time-limited access
* Immediate revocation capability
* Scoped to the issuing user's permissions
* Managed through the OpenL Studio UI

#### **OpenAPI Documentation from Excel**

Rule Services now generates meaningful OpenAPI descriptions directly from Excel documentation. This includes descriptions for datatype fields, method arguments, and spreadsheet result structures.

External consumers can now understand the business meaning of each field and parameter directly from the generated API documentation, reducing the need to consult Excel source files.

#### **Statistical Functions**

New statistical functions have been added to the OpenL Rules engine:

* Standard deviation
* Correlation
* Linear regression

#### **Demo Enhancements**

* Replaced DEMO Docker image with a `compose.yaml` file for easier deployment and configuration
* Reduced the download size of the zip file
* The DEMO package now automatically installs the required JRE, removing the need for manual Java setup

## **Improvements**

### **Deployment & Project Management**

* Simplified deployment workflow by removing the deploy configuration step; users can now deploy projects directly through a streamlined popup dialog
* Project tags are now stored as part of the OpenL project (in `tags.properties` files) rather than in the database, allowing tags to migrate with projects across environments
* Automated OpenL project recognition in Git-based design repositories, removing the need for the "Flat folder structure" checkbox

### **API Enhancements**

* Extended the `/projects/{projectId}/tables` API to support additional table types including Rules, Data, Test, SmartLookup, and SimpleLookup tables
* Added a create table API endpoint
* Improved the get projects/tables API with name filtering, pagination, total count attributes, and an unpaged option for retrieving full lists
* Added a REST API for running tests in OpenL Studio
* Added the ability to inject the current `ProjectDescriptor` into service method handlers

### **Administration & Security**

* Added support for customizing the default user in single-user mode
* Removed the user workspace setting from the UI for security reasons; folder paths can now only be defined via environment variables
* Added WebSocket-based messaging between the admin panel and users for real-time notifications

## **Breaking Changes**

OpenL Tablets 6.0.0 includes significant breaking changes due to the Jakarta EE 10 migration, removal of deprecated features, and simplification of the permission model. Review each section carefully before upgrading.

### **Jakarta EE 10 Namespace Migration (CRITICAL)**

The migration from Java EE to Jakarta EE 10 changes all `javax.*` EE API namespaces to `jakarta.*`. This is the most impactful breaking change in this release.

#### **What Changed**

* All `javax.servlet.*` → `jakarta.servlet.*`
* All `javax.persistence.*` → `jakarta.persistence.*`
* All `javax.validation.*` → `jakarta.validation.*`
* All other `javax.*` EE APIs → corresponding `jakarta.*` equivalents

Major framework upgrades:

* Spring Framework 6.2 (from 5.x)
* Spring Boot 3.4 (from 2.x)
* Spring Security 6.4 (from 5.x)
* Hibernate ORM 6.6 (from 5.x)

#### **Impact**

* All custom Java code using `javax.*` EE packages will fail to compile
* Custom filters, listeners, and interceptors must be updated
* Deployment descriptors referencing `javax` must be updated

#### **Who Is Affected**

You are affected only if your OpenL projects include:

* Custom Java code with `javax.*` EE imports
* Custom servlet filters or listeners
* Custom `web.xml` deployment descriptors
* Direct Hibernate or JPA usage through `javax.persistence`

#### **Migration Steps**

If you are using OpenL Studio without custom Java extensions, no action is required.

The steps below apply only if your projects include custom Java code:

##### **Required namespace changes**

1. Update all `javax.servlet.*` imports to `jakarta.servlet.*`
2. Update all `javax.persistence.*` imports to `jakarta.persistence.*`
3. Update all `javax.validation.*` imports to `jakarta.validation.*`

```java
// Before
import javax.servlet.http.HttpServletRequest;
import javax.persistence.Entity;

// After
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.Entity;
```

##### **JDBC drivers**

OpenL Studio no longer ships with embedded JDBC drivers. Mount the required driver into the Docker container:

```yaml
volumes:
  - ./drivers/postgresql-42.7.8.jar:/opt/openl/lib/postgresql-42.7.8.jar
```

---

### **Variations & Runtime Context Changes**

The Variations feature has been completely removed, and the default value of `isProvideRuntimeContext` has changed to `false`.

#### **What Changed**

* The `isProvideVariations` feature has been removed entirely
* The `isProvideRuntimeContext` default value changed from `true` to `false`
* The `cacheable` property has been removed (used cloning library incompatible with Java 21)
* The `recalculate` property has been removed (designed for the Variation functionality cache)
* The Spreadsheet result calculation step library has been removed

#### **Impact**

* Variation-based endpoints are no longer created
* Projects relying on runtime context injection must explicitly enable it
* Rules relying on variations must be rewritten

#### **Who Is Affected**

You are affected if your projects use:

* `<isProvideVariations>true</isProvideVariations>` in `rules-deploy.xml`
* Runtime context injection without explicit `isProvideRuntimeContext` setting
* The `cacheable` or `recalculate` properties

#### **Migration Steps**

##### **Variations removal**

1. Remove `<isProvideVariations>true</isProvideVariations>` from `rules-deploy.xml`
2. Create a new rule that accepts an array of input parameters and returns an array of results
3. Update integration code to use the new array-based endpoint

##### **Runtime context**

If your project uses runtime context, explicitly enable it:

```xml
<isProvideRuntimeContext>true</isProvideRuntimeContext>
```

---

### **ACL Permission Model Change**

The previous 16-permission model has been replaced with a 3-role model.

#### **What Changed**

* 16 individual permissions consolidated into 3 roles: Viewer, Contributor, Manager
* `security.default-group` no longer grants implicit READ access to all repositories

#### **Impact**

* Effective access may differ after upgrade due to simplified model
* Automation scripts managing permissions via the API must be updated

#### **Who Is Affected**

You are affected if your environment uses:

* Fine-grained permission assignments beyond basic view/edit
* API-based permission management scripts
* Default group configurations for implicit access

#### **Migration Steps**

1. Permissions are migrated automatically during upgrade
2. Review effective permissions after upgrade
3. Update API clients from the old permission endpoints to the new role-based API
4. Review default group settings as they no longer grant implicit READ access

---

### **Removed Features Summary**

The following features, protocols, and APIs have been completely removed from OpenL Tablets 6.0.0:

#### **Removed Protocols & Integration Methods**

* **RMI Protocol**
  * **Migration**: Use REST/SOAP endpoints instead

#### **Removed Database Support**

* **Apache Cassandra**
  * **Migration**: Use JDBC-based store log data module
* **Apache Hive**
  * **Migration**: Use `org.openl.rules.ruleservice.ws.storelogdata.db` module

#### **Removed Libraries & Extensions**

* **Activiti Extension**
  * **Migration**: Use external workflow engine integration via REST API
* **Spreadsheet Result Calculation Step Library**
  * **Migration**: Use `SpreadsheetResult` directly
* **commons-jxpath** (vulnerabilities addressed by removal)

#### **Removed APIs**

* **/rest/repo/\* Endpoints**
  * **Migration**: Use `/rest/projects/*` API instead
* **Deprecated Annotations:**
  * `@AnyType`, `@InjectOpenClass`, `@InjectOpenMember`, `@InjectRulesDeploy`, `@InjectServiceClassLoader`
  * **Migration**: Use standard dependency injection patterns
* **Deprecated Classes:**
  * `StringOperators`, `WholeNumberDivideOperators`, `MulDivNullToOneOperators`
  * **Migration**: Use standard operators
* **Deprecated Methods:**
  * `DoubleRange` and `StringRange` methods including `BoundType` enum, `setLowerBound()`, `setUpperBound()`
  * **Migration**: Use current range API methods
* **Deprecated RulesUtils Methods**
  * **Migration**: Use current alternatives
* **rules.xml versions 5.23 and older**
  * **Migration**: Ensure rules.xml is version 5.24 or later

#### **Removed UI Features**

* **Install Wizard**
  * **Migration**: Initial configuration handled through Admin panel with default single-user setup
* **CAS SSO Authentication**
  * **Migration**: Use OAuth2 or SAML authentication
* **VBScript Support** (following Microsoft's deprecation)
  * **Migration**: No direct replacement; use alternative scripting approaches
* **Java Preferences-Based Configuration**
  * **Migration**: Use environment variables or system properties
* **"Demo" Authentication Mode** in Admin panel
  * **Migration**: Demo setup is preconfigured only in the Demo package

#### **Removed Properties**

* `custom.spreadsheet.type`
* `dispatching.mode`
* `language` property and `OpenL.properties` configurations

## **Security & Library Updates**

### **Security Vulnerability Fixes**

* **CVE-2025-41249**: Spring Framework vulnerability fixed via upgrade to 6.2.11
* **CVE-2025-41248**: Spring Security vulnerability fixed via upgrade to 6.5.5
* Resolved Jackson deserialization vulnerability by using NAME instead of CLASS by default
* Addressed direct vulnerabilities in the commons-jxpath library (library removed)

---

### **Major Library Upgrades**

#### **Runtime Dependencies**

* Amazon AWS SDK 2.34.2
* Angus Mail 2.0.5
* BouncyCastle 1.82
* Glassfish Jakarta Faces 4.0.12
* Gson 2.13.2
* Guava 33.5.0-jre
* Hibernate ORM 6.6.29.Final (from 5.x)
* Jakarta Mail 2.1.5
* Jakarta XML Bind 4.0.4
* JAXB Runtime 4.0.6
* Jetty 12.0.27 (from 11.x)
* Log4j 2.25.2
* Netty 4.2.6.Final
* Nimbus JOSE+JWT 10.5
* OpenSAML 5.1.6 (from 4.x)
* OpenTelemetry 2.20.1
* Spring Boot 3.5.6 (from 2.x)
* Spring Framework 6.2.11 (from 5.x)
* Spring Integration 6.5.2
* Spring Security 6.5.5 (from 5.x)
* Swagger Core 2.2.37
* Swagger Parser 2.1.34

#### **Test Dependencies**

* Mockito 5.20.0
* PostgreSQL Driver 42.7.8
* XMLUnit 2.10.4

#### **Build Tools**

* Apache Maven Compiler Plugin 3.14.1
* Dependency Check Maven Plugin 12.1.6
* License Maven Plugin 2.7.0
* Maven Surefire Plugin 3.5.4
* Rewrite Maven Plugin 6.19.0

#### **Removed Dependencies**

* commons-jxpath (security vulnerabilities)

## **Bug Fixes**

* Fixed an issue where defining a "Default Group" would grant READ permission to all projects
* Fixed Git LFS not working with Bitbucket repositories
* Fixed OpenL Studio working incorrectly or crashing when a project with an OpenAPI file had a dependency on other projects
* Fixed an Internal Server Error when deploying a project without specifying `base.path` in the configuration
* Fixed OAuth2 not working with the default configuration
* Fixed an `IllegalStateException` in Trace when a rule had a version with the `Origin='Deviation'` property
* Fixed dependency graph display for tables containing business dimensional properties
* Fixed table connection display on the dependencies graph when one table calls another using Multi Call

## **Migration Notes**

### **Quick Role-Based Pointers**

* **If you are a Rules Author** → pay special attention to sections **Rules & Rule Execution**, **Metadata & OpenAPI**
* **If you are a Developer** → pay special attention to sections **Runtime Environment**, **Rules & Rule Execution**, **Integration & Serialization**
* **If you are an Administrator / Platform Owner** → pay special attention to sections **Access Control & Permissions**, **Runtime Environment**, **Administration, Deployment & Removed Features**

---

### **Access Control & Permissions**

* The legacy 16-permission model has been replaced with role-based access control (RBAC) using 3 roles: Viewer, Contributor, Manager.
  Permissions are migrated automatically, but effective access may differ after upgrade.
* `security.default-group` no longer grants implicit READ access to repositories.
* Project creation and deletion are additionally controlled by the system property:

```properties
security.allow-project-create-delete
```

* Role assignments and default group permissions should be reviewed after upgrade.

#### **User Access & Permission Mapping**

| Legacy Permission | New Behavior | Notes / Action |
| :---- | :---- | :---- |
| **View Projects** | Included in all roles | Deprecated; project viewing available to all roles |
| **Edit Projects** | Included in Contributor role | Covered by the Edit permission |
| **Create/Delete Projects** | Controlled by system property | Set `security.allow-project-create-delete` |
| **Administrative** | Manager role | Full control including role assignment |

---

### **Runtime Environment**

* Java **21** is required. Earlier Java versions are no longer supported.
* All `javax.*` packages have been replaced with `jakarta.*`.
  Custom Java code must be updated accordingly.
* Embedded JDBC drivers have been removed from the distribution.
  Mount the required driver JAR into `/opt/openl/lib` in Docker deployments.

---

### **Rules & Rule Execution**

* The **variations** feature has been completely removed.
  * Variation-based endpoints are no longer created.
  * Rules relying on variations must be rewritten as array-based rules.
* `isProvideRuntimeContext` now defaults to `false`.
  * Projects using runtime context must explicitly set it to `true` in `rules-deploy.xml`.
* The `cacheable` and `recalculate` properties have been removed.

---

### **Metadata & OpenAPI**

* Project tags are now stored inside the project structure (in `tags.properties` files).
  * Tags are migrated automatically during upgrade.
  * Tag changes are now version-controlled along with the project.

---

### **Integration & Serialization**

* JSON polymorphic serialization now uses **NAME-based** type identification instead of CLASS-based.
  * Client applications relying on CLASS-based typing may require updates.

---

### **Administration, Deployment & Removed Features**

* The following components are no longer supported:
  * RMI protocol
  * CAS SSO authentication
  * Legacy `/rest/repo/*` API endpoints
  * Install Wizard
  * VBScript execution
  * Java Preferences-based configuration
* The Administration UI has been fully redesigned with React.
  * No functional loss; familiarize yourself with the new layout.
* The "Demo" authentication mode has been removed from the Admin panel.
  * Demo setup is now preconfigured only in the Demo package.

# OpenL Tablets 6.0.0 Release Notes

**Release Date:** January 2026

OpenL Tablets 6.0.0 represents the most significant modernization of the platform in years, bringing the enterprise business rules engine fully into the modern Java ecosystem. This major release focuses on three key themes: **platform modernization** with Java 21 and Jakarta EE 10, **improved user experience** with a completely redesigned React-based administration interface, and **simplified architecture** through the removal of legacy features and consolidation of internal components.

Organizations upgrading to 6.0.0 will benefit from improved performance, enhanced security through updated dependencies, a more intuitive administration experience, and better alignment with current enterprise Java standards. However, this release includes significant breaking changes that require careful planning and migration.

> **Important**: This is a major release with breaking changes. Review the [Breaking Changes](#3-breaking-changes) and [Migration Notes](#7-migration-notes) sections before upgrading.

---

## 1. New Features

### 1.1 Java 21 and Modern Runtime Support

OpenL Tablets 6.0.0 establishes **Java 21 as the minimum required runtime**, enabling the platform to leverage modern language features, improved performance characteristics, and enhanced security. This change positions OpenL Tablets for long-term support as Java 21 is an LTS (Long-Term Support) release.

The platform has been thoroughly tested and validated to support **Java versions up to Java 25**, ensuring organizations have flexibility in their JDK choices and can plan for future upgrades. All core components, bytecode generation, and runtime execution have been optimized for modern JVM implementations.

#### Benefits of Java 21

* **Virtual threads support**: Improved concurrency handling for high-throughput rule services
* **Pattern matching enhancements**: Cleaner internal code and better performance
* **Improved garbage collection**: Better memory management for large rule compilations
* **Enhanced security**: Access to the latest security fixes and cryptographic improvements
* **Longer support lifecycle**: Java 21 LTS provides support through 2029

#### Migration Notes

Before upgrading to OpenL Tablets 6.0.0, ensure your runtime environment meets the minimum requirements:

1. Upgrade your JDK to Java 21 or later
2. Update any JAVA_HOME environment variables
3. Verify your deployment infrastructure (containers, application servers) supports Java 21
4. Test custom Java extensions for compatibility with the new runtime

### 1.2 Jakarta EE 10 Migration

OpenL Tablets has completed a comprehensive migration from **Java EE (javax.\*) to Jakarta EE 10 (jakarta.\*)**, aligning with the modern enterprise Java ecosystem. This migration affects all web, persistence, validation, and servlet APIs throughout the platform.

Jakarta EE 10 brings modern API improvements, better cloud-native support, and ensures compatibility with current and future versions of application servers and frameworks. The migration also enables OpenL Tablets to leverage Spring Framework 6.x and Spring Boot 3.x, which require Jakarta EE namespaces.

#### What Changed

* All `javax.servlet.*` packages are now `jakarta.servlet.*`
* All `javax.persistence.*` packages are now `jakarta.persistence.*`
* All `javax.validation.*` packages are now `jakarta.validation.*`
* All `javax.annotation.*` packages are now `jakarta.annotation.*`
* XML namespace declarations have been updated to Jakarta EE schemas

#### Impact on Custom Extensions

If you have developed custom extensions, integrations, or rule interceptors that use Java EE APIs, you must update the import statements:

```java
// Before (Java EE)
import javax.servlet.http.HttpServletRequest;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

// After (Jakarta EE 10)
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
```

#### Migration Notes

1. Update all import statements from `javax.*` to `jakarta.*` in custom code
2. Update Maven dependencies to Jakarta EE 10 versions
3. If using XML configuration files, update namespace URIs to Jakarta EE schemas
4. Rebuild and test all custom extensions before deployment

### 1.3 Redesigned React-Based Administration Interface

OpenL Tablets 6.0.0 introduces a **completely redesigned administration interface** built with modern React technology, replacing the legacy JSF/RichFaces implementation. The new interface provides a significantly improved user experience for managing users, groups, security settings, and repositories.

The new administration UI was designed with usability as the primary focus, featuring a clean, responsive layout that works well on various screen sizes and follows modern web application conventions. Common administrative tasks that previously required multiple page navigations can now be accomplished with fewer clicks and better visual feedback.

#### User and Group Management

The redesigned user management interface provides:

* **Streamlined user creation and editing** with inline validation
* **Group membership management** with drag-and-drop functionality
* **Member count display** for each group, providing quick visibility into group sizes
* **Bulk operations** for common tasks like enabling/disabling users
* **Search and filtering** with case-insensitive partial matching across all fields

#### Repository Configuration

Repository management has been enhanced with:

* **Visual repository configuration** with real-time validation
* **Connection testing** before saving repository settings
* **Support for multiple repository types** including Git, database, AWS S3, Azure Blob, and local file system
* **Design and production repository** configuration in a unified interface
* **ACL import/export** for backing up and restoring access control settings

#### Security Configuration

Security settings are now managed through a dedicated interface:

* **OAuth2/OIDC configuration** with simplified setup wizards
* **SAML configuration** for enterprise SSO integration
* **LDAP/Active Directory** integration settings
* **Local authentication** management

#### Real-Time Notifications

The new interface implements **WebSocket-based real-time notifications**, replacing the previous polling-based approach. Users now receive immediate feedback on:

* Repository synchronization status
* Deployment progress and completion
* System events and alerts
* Concurrent editing notifications

### 1.4 New Deployment REST API

OpenL Tablets 6.0.0 introduces a **comprehensive Deployment REST API** that provides programmatic control over the deployment process. This API replaces the legacy Deploy Configuration feature and enables integration with CI/CD pipelines, automation scripts, and external deployment tools.

The new API follows RESTful design principles with proper HTTP method semantics, meaningful status codes, and JSON request/response formats. It supports both synchronous and asynchronous deployment operations, with progress tracking for long-running deployments.

#### Key Capabilities

* **Deploy projects** to production repositories with a single API call
* **Monitor deployment status** through polling or WebSocket notifications
* **Rollback deployments** to previous versions
* **Query deployment history** for audit and troubleshooting
* **Configure deployment options** such as target repositories and version handling

#### React Modal Dialog

For interactive deployments, the new React UI includes a modal deployment dialog that:

* Displays deployment progress in real-time
* Shows validation results before deployment
* Provides clear success/failure feedback
* Supports deployment to multiple production repositories

#### Integration Example

```bash
# Deploy a project using the new REST API
curl -X POST "https://studio.example.com/webstudio/api/v1/deployments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "projectName": "MyRulesProject",
    "targetRepository": "production",
    "version": "1.2.0"
  }'
```

### 1.5 Enhanced Tables API with Pagination

The Tables API has been enhanced with **full pagination support**, enabling efficient handling of projects with large numbers of tables. This is particularly important for enterprise deployments where rule projects may contain hundreds or thousands of individual tables.

#### Pagination Features

* **Configurable page size** through the `size` query parameter
* **Total element count** returned in responses for client-side pagination controls
* **Unpaged mode** available for backwards compatibility or when full results are needed
* **Consistent pagination** across all table listing endpoints

#### API Usage

```bash
# Fetch page 2 with 50 tables per page
GET /api/v1/projects/{projectId}/tables?page=2&size=50

# Response includes pagination metadata
{
  "content": [...],
  "totalElements": 523,
  "totalPages": 11,
  "currentPage": 2,
  "pageSize": 50
}
```

### 1.6 Simplified ACL and Permissions Model

While maintaining enterprise-grade security, OpenL Tablets 6.0.0 simplifies the access control model to reduce configuration complexity. The permission system has been streamlined to focus on the most commonly used access patterns.

#### Key Changes

* **Consolidated permission types** for clearer role definitions
* **Project-level and repository-level** ACL management
* **ACL import/export API** for backup and migration
* **Removed deprecated ACL endpoints** in favor of the new unified API

#### ACL Import/Export

Administrators can now export and import ACL configurations:

```bash
# Export ACL configuration
GET /api/v1/admin/acl/export

# Import ACL configuration
POST /api/v1/admin/acl/import
Content-Type: application/json
{
  "permissions": [...]
}
```

---

## 2. Improvements

### Core Engine

* **Simplified InstantiationStrategy hierarchy**: Merged `CommonRulesInstantiationStrategy` into `SimpleMultiModuleInstantiationStrategy`, reducing complexity for users working with multi-module projects
* **Unified method handler**: Merged `OpenLMethodHandler` into `OpenLRulesMethodHandler` for cleaner method handling and reduced code duplication
* **Module consolidation**: Core modules (grammars, core) merged into the rules module for simplified architecture and faster build times
* **Removed JGraphT dependency**: Replaced with a simpler built-in implementation, reducing external dependencies
* **Removed Spring dependency**: The `org.openl.rules.project` module no longer depends on Spring Framework, enabling lighter deployments
* **ISO-8601 date formatting**: String presentation of Date cell values now uses ISO-8601 format for better internationalization

### OpenL Studio

* **Case-insensitive filtering**: Selectors now support case-insensitive partial match filtering for improved usability
* **OAuth2/OIDC compliance**: User identity now uses the 'sub' claim per OIDC specification, improving interoperability with identity providers
* **Project tags persistence**: Tags are now saved to files, surviving restarts and repository operations
* **Administrator status indicator**: User profiles now display a flag indicating administrator status
* **External scripts support**: Settings responses include external scripts configuration for extensibility
* **Spring Security 6 migration**: Moved from XML-based to Java-based security configuration for better maintainability

### Rule Services

* **Spring 6.x integration**: Updated validation message handling and OpenAPI configuration for Spring 6 compatibility
* **Improved classloader handling**: Uses correct classloader for Spring Context in complex deployment scenarios
* **Simplified OpenAPI schemas**: Internal endpoints now use @Hidden annotation for cleaner API documentation
* **ProjectDescriptor access**: Custom handlers can now access ProjectDescriptor (rules.xml) for advanced customization

### Build and Infrastructure

* **EditorConfig standardization**: Added EditorConfig for consistent coding styles across the project
* **Automated dependency updates**: Configured Dependabot for automated dependency updates across multiple package ecosystems
* **Academic citation support**: Added CITATION.cff for proper academic citation of OpenL Tablets
* **Contribution guidelines**: Added CONTRIBUTING.md with project contribution guidelines
* **Code of Conduct**: Added Contributor Covenant Code of Conduct
* **Beta release automation**: GitHub workflow for automated beta releases
* **Improved Docker images**: Updated to use Alpine base image with reduced vulnerabilities
* **Podman support**: Support for podman in rootless mode for containerized deployments

---

## 3. Breaking Changes

### 3.1 Java 21 Minimum Requirement

OpenL Tablets 6.0.0 requires Java 21 or later. Java 11, 17, and earlier versions are no longer supported.

#### What Changed

* Minimum Java version increased from Java 11 to Java 21
* All bytecode generation targets Java 21
* Some internal APIs use Java 21+ language features

#### Impact

* Applications running on Java 11 or 17 will not start
* Docker images and deployment environments must be updated
* Build systems must use JDK 21+

#### Migration Steps

1. Install JDK 21 or later (recommended: Eclipse Temurin or Amazon Corretto)
2. Update `JAVA_HOME` environment variable
3. Update Docker base images to use Java 21
4. Update CI/CD pipelines to use Java 21
5. Rebuild any custom extensions with JDK 21

### 3.2 Jakarta EE 10 Namespace Migration

All Java EE `javax.*` packages have been replaced with Jakarta EE `jakarta.*` packages.

#### What Changed

* All servlet APIs: `javax.servlet.*` to `jakarta.servlet.*`
* All persistence APIs: `javax.persistence.*` to `jakarta.persistence.*`
* All validation APIs: `javax.validation.*` to `jakarta.validation.*`
* All annotation APIs: `javax.annotation.*` to `jakarta.annotation.*`

#### Impact

* Custom interceptors, filters, and servlets must be updated
* Custom entity classes must update annotations
* XML configuration files need namespace updates

#### Migration Steps

1. Search your codebase for `import javax.` statements
2. Replace with corresponding `import jakarta.` statements
3. Update XML namespaces in configuration files
4. Update Maven dependencies to Jakarta EE 10 versions
5. Rebuild and test thoroughly

### 3.3 Removed CAS SSO Support

CAS (Central Authentication Service) Single Sign-On support has been removed from OpenL Tablets.

#### What Changed

* All CAS-related configuration properties are ignored
* CAS authentication filters have been removed
* CAS client libraries are no longer included

#### Why This Changed

CAS adoption has declined significantly in favor of OAuth2/OIDC and SAML, which provide better security, wider tooling support, and more flexible integration options.

#### Migration Steps

1. Evaluate OAuth2/OIDC providers (Keycloak, Okta, Azure AD, etc.)
2. Configure your CAS server to expose an OAuth2/OIDC interface if possible
3. Update OpenL Tablets to use OAuth2/OIDC authentication
4. Test authentication flows thoroughly before production deployment

### 3.4 Removed Variations Support

The rule variations feature has been removed from OpenL Tablets.

#### What Changed

* Variations API endpoints have been removed
* Variation execution is no longer supported
* Variation-related configuration properties are ignored

#### Why This Changed

The variations feature saw limited adoption and added significant complexity to the codebase. The functionality can be achieved through other means such as parameterized rules or separate rule versions.

#### Migration Steps

1. Identify rules that use variations
2. Convert variations to parameterized rules or separate rule versions
3. Update calling code to use the new approach
4. Test thoroughly to ensure equivalent behavior

### 3.5 Removed Deploy Configuration

The legacy Deploy Configuration feature has been replaced by the new Deployment REST API.

#### What Changed

* Deploy Configuration UI has been removed
* Legacy deployment endpoints are no longer available
* Deploy configuration files are ignored

#### Migration Steps

1. Migrate deployment scripts to use the new Deployment REST API
2. Update CI/CD pipelines to use API-based deployment
3. Use the new React deployment dialog for manual deployments

### 3.6 Removed Install Wizard

The Install Wizard has been removed from OpenL Tablets.

#### What Changed

* The Install Wizard UI is no longer available
* Initial configuration must be done via configuration files or environment variables

#### Migration Steps

1. Configure OpenL Tablets using `application.properties` or environment variables
2. Use the provided configuration templates as a starting point
3. For containerized deployments, use environment variables for configuration

### 3.7 Removed RMI Protocol Support

RMI (Remote Method Invocation) protocol support has been removed from Rule Services.

#### What Changed

* RMI endpoints are no longer available
* RMI-related configuration properties are ignored
* RMI client libraries are not included

#### Why This Changed

RMI is a legacy protocol with security concerns and limited firewall friendliness. Modern alternatives like REST and gRPC provide better security, performance, and tooling support.

#### Migration Steps

1. Migrate RMI clients to REST or gRPC
2. Update service configuration to expose REST or gRPC endpoints
3. Update client applications to use the new protocols

### 3.8 Removed Cassandra and Hive Support

Apache Cassandra and Apache Hive repository support has been removed.

#### What Changed

* Cassandra repository implementation removed
* Hive repository implementation removed
* Related configuration properties are ignored

#### Migration Steps

1. Migrate data from Cassandra/Hive to supported repositories (Git, database, S3, etc.)
2. Update repository configuration to use a supported type
3. Test rule retrieval and compilation with the new repository

### 3.9 JSON Serialization Type Format Change

The default JSON polymorphic type identifier has changed from CLASS to NAME.

#### What Changed

* `ruleservice.jackson.jsonTypeInfoId` now defaults to `NAME` instead of `CLASS`
* JSON type information uses simple type names instead of fully qualified class names

#### Impact

* Existing JSON data with CLASS-based types may not deserialize correctly
* Clients expecting CLASS-based type identifiers must be updated

#### Migration Steps

1. Update client applications to handle NAME-based type identifiers
2. If you must retain CLASS format (not recommended), set:
   ```properties
   ruleservice.jackson.jsonTypeInfoId=CLASS
   ```
3. Consider migrating stored JSON data to the new format

### 3.10 Removed Legacy APIs

Several deprecated APIs have been removed:

#### Removed Endpoints

* `/rest/admin/management/groups/settings` - Use new ACL API
* `/rest/admin/management/old/groups` - Use new groups API
* `/rest/repo/*` - Use new repository management APIs

#### Removed Configuration Properties

* `user.mode=demo` - Demo mode removed
* `custom.spreadsheet.type` - Custom spreadsheet types no longer supported
* `dispatching.mode` - Dispatching mode removed
* `cacheable` property - Use alternative caching mechanisms
* `recalculate` property - Use alternative approaches

#### Removed Features

* Embedded JDBC drivers - Provide drivers externally
* User Workspace Home setting in UI - Use configuration files
* SpreadsheetResult converters - Use standard serialization
* Support for projects older than version 5.25
* `OpenL.properties` configuration files

---

## 4. Security and Library Updates

### 4.1 Security Enhancements

* **Java 21 security improvements**: Access to the latest JDK security features and fixes
* **Updated cryptographic libraries**: Bouncy Castle upgraded to 1.83
* **Improved OAuth2/OIDC compliance**: Follows OIDC specification for user identity
* **Alpine-based Docker images**: Reduced attack surface with minimal base images
* **Node.js security update**: Upgraded to Node.js 24.13.0 to address vulnerabilities

### 4.2 Major Library Upgrades

| Library | Previous Version | New Version |
|---------|------------------|-------------|
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
| Swagger Core | 2.2.x | 2.2.42 |
| Swagger Parser | 2.1.x | 2.1.37 |
| Log4j | 2.23.x | 2.25.3 |
| Groovy | 4.0.x | 4.0.30 |
| Nimbus JOSE+JWT | 9.x | 10.7 |
| AWS SDK | 2.26.x | 2.41.18 |
| Azure Storage Blob | 12.27.x | 12.33.1 |
| Netty | 4.1.x | 4.2.9.Final |
| H2 Database | 2.2.x | 2.4.240 |
| JGit | 6.10.x | 7.3.0.202506031305-openl |
| TestContainers | 1.19.x | 2.0.3 |
| JUnit | 5.10.x | 6.0.2 |
| Mockito | 5.12.x | 5.21.0 |
| Commons Codec | 1.17.x | 1.21.0 |
| Commons IO | 2.16.x | 2.21.0 |
| Commons Lang3 | 3.14.x | 3.20.0 |
| Bouncy Castle | 1.78.x | 1.83 |
| AspectJ | 1.9.22.x | 1.9.25.1 |
| JCodeModel | 3.4.x | 4.1.0 |
| Node.js | 22.x | 24.13.0 |
| PostgreSQL JDBC | 42.7.x | 42.7.9 |
| Oracle JDBC | 23.4.x | 23.26.0.0.0 |
| MS SQL JDBC | 12.6.x | 13.2.1.jre11 |
| MariaDB JDBC | 2.7.x | 2.7.13 |
| Kafka | 3.7.x | 4.1.1 |
| XMLUnit | 2.10.x | 2.11.0 |
| Greenmail | 2.0.x | 2.1.8 |
| Minio | 8.5.x | 8.6.0 |

---

## 5. Bug Fixes

### Core Engine

* **DoubleRange NaN handling**: Fixed `DoubleRange.contains()` to properly handle NaN values according to IEEE 754 semantics, preventing incorrect range evaluations

### OpenL Studio

* **Method graph for overloaded tables**: Fixed method graph visualization for overloaded decision tables, now correctly displaying all overload variants
* **Multicall method dependencies**: Fixed method graph visualization for multicall method dependencies, showing accurate dependency relationships
* **Project dependencies with database repositories**: Fixed NullPointerException when using project dependencies with non-branch enabled database repositories
* **OAuth2 settings update**: Fixed issue where updating OAuth2 settings required re-entering the Client Secret
* **First repository creation**: Fixed adding the first repository in fresh installations
* **Projects REST API**: Fixed projects REST API request handling
* **Remote repository URI**: Fixed defining remote repository URI in repository configuration
* **Help page navigation**: Fixed direct navigation to the Help page
* **Repository page crash**: Fixed repository page crash when 'repository.name' is configured as read-only
* **AccessDeniedHandler execution**: Fixed unpredictable order of AccessDeniedHandler execution

### Build and Infrastructure

* **Windows 11 build**: Fixed building under Windows 11 with webpack 6.0.0 features
* **Java 25 DEMO package**: Fixed configuration for DEMO package under Java 25
* **Groovy scripts**: Fixed Groovy scripts compatibility with updated dependencies
* **AWS SDK regions**: Fixed test compatibility with new AWS regions added in recent AWS SDK versions

---

## 6. Removed Features Summary

The following features have been removed in OpenL Tablets 6.0.0. See [Breaking Changes](#3-breaking-changes) for migration guidance.

| Feature | Replacement |
|---------|-------------|
| CAS SSO Support | OAuth2/OIDC or SAML |
| Variations Support | Parameterized rules or separate rule versions |
| Deploy Configuration | Deployment REST API |
| Install Wizard | Configuration files or environment variables |
| RMI Protocol | REST or gRPC |
| Cassandra Repository | Git, database, S3, or Azure Blob |
| Hive Repository | Git, database, S3, or Azure Blob |
| Activity Logging | External logging solutions |
| Embedded JDBC Drivers | Provide drivers externally |
| Legacy ACL API | New ACL Projects and Repository APIs |
| Legacy Repository API | New repository management APIs |
| User Workspace Home UI Setting | Configuration files |
| Demo User Mode | Standard authentication |
| Custom Spreadsheet Type | Standard spreadsheet handling |
| Dispatching Mode | Standard dispatching |
| SpreadsheetResult Converters | Standard serialization |
| Cacheable Property | Alternative caching mechanisms |
| Recalculate Property | Alternative approaches |
| OpenL Assistant Integration | New assistant integration (coming soon) |
| Deprecated Operators | Standard operators |
| Projects < 5.25 | Upgrade projects to 5.25+ format |
| OpenL.properties | application.properties |

---

## 7. Migration Notes

### 7.1 Pre-Upgrade Checklist

Before upgrading to OpenL Tablets 6.0.0:

- [ ] Verify Java 21 or later is installed
- [ ] Backup all repositories and databases
- [ ] Document current configuration settings
- [ ] Identify custom extensions and integrations
- [ ] Review removed features and plan alternatives
- [ ] Test upgrade in a non-production environment first

### 7.2 Java Runtime Migration

1. **Install Java 21**: Download and install JDK 21 (Eclipse Temurin, Amazon Corretto, or Oracle JDK)
2. **Update environment**: Set `JAVA_HOME` to the new JDK path
3. **Update containers**: Use Java 21 base images for Docker deployments
4. **Update build tools**: Ensure Maven/Gradle use JDK 21

### 7.3 Jakarta EE Migration

If you have custom Java code extending OpenL Tablets:

1. **Find affected imports**: Search for `import javax.servlet`, `import javax.persistence`, etc.
2. **Replace namespaces**: Change `javax.*` to `jakarta.*`
3. **Update dependencies**: Use Jakarta EE 10 compatible versions
4. **Rebuild**: Compile with JDK 21 and the updated dependencies

### 7.4 Authentication Migration

**From CAS to OAuth2/OIDC:**

1. Set up an OAuth2/OIDC provider (Keycloak, Okta, Azure AD, etc.)
2. Configure OpenL Tablets with OAuth2 settings:
   ```properties
   security.oauth2.client-id=your-client-id
   security.oauth2.client-secret=your-client-secret
   security.oauth2.authorization-uri=https://provider/oauth2/authorize
   security.oauth2.token-uri=https://provider/oauth2/token
   security.oauth2.user-info-uri=https://provider/oauth2/userinfo
   ```
3. Test authentication flow
4. Migrate user mappings if necessary

### 7.5 API Migration

**Legacy API to New API mapping:**

| Old Endpoint | New Endpoint |
|--------------|--------------|
| `/rest/repo/*` | `/api/v1/repositories/*` |
| `/rest/admin/management/groups/settings` | `/api/v1/admin/acl/*` |
| `/rest/admin/management/old/groups` | `/api/v1/admin/groups/*` |
| Deploy Configuration | `/api/v1/deployments/*` |

### 7.6 Repository Migration

If using Cassandra or Hive:

1. Export rules from the current repository
2. Set up a new supported repository (Git, database, S3, etc.)
3. Import rules into the new repository
4. Update configuration to point to the new repository
5. Test rule compilation and execution

### 7.7 JDBC Driver Configuration

JDBC drivers are no longer bundled. To use database repositories:

1. Download the JDBC driver for your database
2. Place the driver JAR in the classpath:
   - For standalone: `lib/` directory
   - For Docker: Mount as a volume or extend the image
3. Restart OpenL Tablets

### 7.8 Post-Upgrade Verification

After upgrading:

1. **Verify startup**: Check logs for errors or warnings
2. **Test authentication**: Verify users can log in
3. **Test rule compilation**: Compile a sample project
4. **Test rule execution**: Execute rules through Rule Services
5. **Test deployment**: Deploy a project to production
6. **Verify integrations**: Test any external integrations

---

## 8. Getting Help

### Documentation

* **User Guide**: https://openl-tablets.org/documentation
* **Configuration Reference**: https://openl-tablets.org/documentation/configuration
* **API Documentation**: https://openl-tablets.org/documentation/api

### Support Channels

* **Community Forum**: https://openl-tablets.org/forum
* **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
* **Commercial Support**: contact@openl-tablets.org

### Live Demo

Try OpenL Tablets online: https://openl-tablets.org/demo

---

## 9. Acknowledgments

This release includes contributions from the OpenL Tablets community. Special thanks to all contributors who submitted bug reports, feature requests, and pull requests.

---

**GitHub Release**: https://github.com/openl-tablets/openl-tablets/releases/tag/6.0.0

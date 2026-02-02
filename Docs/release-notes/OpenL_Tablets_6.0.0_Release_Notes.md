# OpenL Tablets 6.0.0 Release Notes

**Release Date:** January 2026

[v6.0.0 on GitHub](https://github.com/openl-tablets/openl-tablets/releases/tag/6.0.0)

OpenL Tablets 6.0.0 represents the most significant modernization of the platform in years, bringing the enterprise business rules engine fully into the modern Java ecosystem. This major release focuses on three key themes: **platform modernization** with Java 21 and Jakarta EE 10, **improved user experience** with a completely redesigned React-based administration interface, and **simplified architecture** through the removal of legacy features and consolidation of internal components.

Organizations upgrading to 6.0.0 will benefit from improved performance, enhanced security through updated dependencies, a more intuitive administration experience, and better alignment with current enterprise Java standards. However, this release includes significant breaking changes that require careful planning and migration.

> **Important**: This is a major release with breaking changes. Review the [Breaking Changes](#breaking-changes) and [Migration Notes](#migration-notes) sections before upgrading.

---

## New Features

### OpenL Core

#### Java 21 and Modern Runtime Support

**Overview**

OpenL Tablets 6.0.0 establishes Java 21 as the minimum required runtime, enabling the platform to leverage modern language features, improved performance characteristics, and enhanced security. This change positions OpenL Tablets for long-term support as Java 21 is an LTS (Long-Term Support) release.

**Key Components**

- **Virtual threads support**: Improved concurrency handling for high-throughput rule services
- **Pattern matching enhancements**: Cleaner internal code and better performance
- **Improved garbage collection**: Better memory management for large rule compilations
- **Enhanced security**: Access to the latest security fixes and cryptographic improvements
- **Longer support lifecycle**: Java 21 LTS provides support through 2029
- **Java 25 compatibility**: Platform validated for Java versions up to Java 25

**Benefits**

The move to Java 21 positions OpenL Tablets for long-term enterprise support while enabling access to modern JVM improvements. Organizations gain improved performance, better memory management, and enhanced security through access to the latest JDK features and security patches.

**Technical Details**

All core components, bytecode generation, and runtime execution have been optimized for modern JVM implementations. The platform uses the latest ASM 9.9.1 library for bytecode generation, ensuring compatibility with Java 21+ class file formats.

---

#### Jakarta EE 10 Migration

**Overview**

OpenL Tablets has completed a comprehensive migration from Java EE (javax.*) to Jakarta EE 10 (jakarta.*), aligning with the modern enterprise Java ecosystem. This migration affects all web, persistence, validation, and servlet APIs throughout the platform.

**Key Components**

- **Servlet APIs**: All `javax.servlet.*` packages migrated to `jakarta.servlet.*`
- **Persistence APIs**: All `javax.persistence.*` packages migrated to `jakarta.persistence.*`
- **Validation APIs**: All `javax.validation.*` packages migrated to `jakarta.validation.*`
- **Annotation APIs**: All `javax.annotation.*` packages migrated to `jakarta.annotation.*`
- **XML namespaces**: All configuration files updated to Jakarta EE schemas

**Benefits**

Jakarta EE 10 brings modern API improvements, better cloud-native support, and ensures compatibility with current and future versions of application servers and frameworks. The migration also enables OpenL Tablets to leverage Spring Framework 6.x and Spring Boot 3.x, which require Jakarta EE namespaces.

**Technical Details**

The migration required updates across all modules including web services, studio, and rule services components. Spring Boot has been upgraded to 3.5.10 and Spring Framework to 6.2.15, both requiring Jakarta EE 10 namespaces.

---

#### Simplified InstantiationStrategy Architecture

**Overview**

The internal architecture for rule instantiation has been significantly simplified by merging `CommonRulesInstantiationStrategy` into `SimpleMultiModuleInstantiationStrategy` and consolidating `OpenLMethodHandler` into `OpenLRulesMethodHandler`. This reduces complexity for users working with multi-module projects and provides a cleaner API.

**Key Components**

- **Unified method handler**: Single handler for all OpenL rules method invocation
- **Simplified strategy hierarchy**: Reduced class hierarchy for rule instantiation
- **Modern runtime context**: New approach for defining runtime context without IEngineWrapper hacking
- **Improved classloader handling**: Correct classloader used for binding imports from OpenL rules

**Benefits**

Users working with multi-module projects benefit from a simpler, more intuitive API. The consolidation reduces the learning curve and eliminates confusion about which strategy or handler to use in different scenarios.

---

### OpenL Studio

#### Redesigned React-Based Administration Interface

**Overview**

OpenL Tablets 6.0.0 introduces a completely redesigned administration interface built with modern React technology, replacing the legacy JSF/RichFaces implementation. The new interface provides a significantly improved user experience for managing users, groups, security settings, and repositories.

**Key Components**

- **Security Settings**: Manage authentication modes (Multi-user, Active Directory, SAML, OAuth2) with improved configuration interface
- **User Management**: Enhanced user creation, editing, and deletion with improved validation and error handling
- **Group Management**: Streamlined group administration with member count display and quick access to group details
- **Repository Management**: Intuitive repository configuration with support for Git, AWS S3, Azure Blob, and JDBC repositories, including connection testing and improved error reporting
- **System Settings**: Centralized management of system properties including date formats, time formats, auto-compile settings, and thread pool configurations

**Benefits**

The new administration UI was designed with usability as the primary focus, featuring a clean, responsive layout that works well on various screen sizes and follows modern web application conventions. Common administrative tasks that previously required multiple page navigations can now be accomplished with fewer clicks and better visual feedback.

**Technical Details**

The new UI is built using React 18 with modern component architecture, providing real-time updates through WebSocket connections. The interface follows responsive design principles and is compatible with all modern browsers. All administrative operations are backed by RESTful APIs, enabling potential automation and integration scenarios.

---

#### Simplified ACL and Permissions Model

**Overview**

While maintaining enterprise-grade security, OpenL Tablets 6.0.0 simplifies the access control model to reduce configuration complexity. The permission system has been streamlined to focus on the most commonly used access patterns with consolidated permission types for clearer role definitions.

**Key Components**

- **Consolidated permission types**: Simplified model with clearer role definitions
- **Project-level ACL**: Access control at the project level for granular permissions
- **Repository-level ACL**: Access control at the repository level for broader permissions
- **ACL Import/Export API**: REST API for backup and migration of ACL configurations
- **Unified ACL API**: New consolidated API replacing deprecated endpoints

**Benefits**

Administrators spend less time configuring complex permission matrices and more time ensuring users have appropriate access. The streamlined model is easier to understand, document, and audit while still providing the security controls enterprises require.

**Technical Details**

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

#### Real-Time WebSocket Notifications

**Overview**

The new interface implements WebSocket-based real-time notifications, replacing the previous polling-based approach. Users now receive immediate feedback on system events without page refreshes.

**Key Components**

- **Repository synchronization status**: Real-time updates on sync progress
- **Deployment progress and completion**: Live deployment status updates
- **System events and alerts**: Immediate notification of important events
- **Concurrent editing notifications**: Awareness of other users editing the same resources

**Benefits**

Users receive immediate feedback on operations, improving the responsiveness of the application and reducing uncertainty about the status of long-running operations.

---

### OpenL Studio Admin

#### New Deployment REST API

**Overview**

OpenL Tablets 6.0.0 introduces a comprehensive Deployment REST API that provides programmatic control over the deployment process. This API replaces the legacy Deploy Configuration feature and enables integration with CI/CD pipelines, automation scripts, and external deployment tools.

**Key Components**

- **Deploy projects**: Deploy to production repositories with a single API call
- **Monitor deployment status**: Polling or WebSocket notifications for progress tracking
- **Rollback deployments**: Restore to previous versions when needed
- **Query deployment history**: Audit and troubleshooting capabilities
- **Configure deployment options**: Target repositories and version handling

**Benefits**

The new API follows RESTful design principles with proper HTTP method semantics, meaningful status codes, and JSON request/response formats. It supports both synchronous and asynchronous deployment operations, with progress tracking for long-running deployments. DevOps teams can now integrate OpenL deployments directly into their CI/CD pipelines.

**Technical Details**

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

---

#### React Modal Deployment Dialog

**Overview**

For interactive deployments, the new React UI includes a modal deployment dialog that provides a modern, intuitive deployment experience directly from the Studio interface.

**Key Components**

- **Real-time progress display**: Live updates during deployment process
- **Validation results**: Pre-deployment checks with clear pass/fail indicators
- **Success/failure feedback**: Clear visual indication of deployment outcome
- **Multi-repository deployment**: Support for deploying to multiple production repositories

**Benefits**

Users no longer need to navigate away from their current context to initiate deployments. The modal dialog provides all necessary information and controls in a single, focused interface.

---

#### Merge API with React UI

**Overview**

A new React-based UI for the Merge API enables users to manage branch merges and conflict resolution through an intuitive visual interface.

**Key Components**

- **Visual merge interface**: Clear representation of merge operations
- **Conflict resolution**: Interactive conflict resolution tools
- **Merge preview**: Preview changes before completing the merge
- **History tracking**: Complete audit trail of merge operations

---

### OpenL Rule Services

#### Enhanced Tables API with Pagination

**Overview**

The Tables API has been enhanced with full pagination support, enabling efficient handling of projects with large numbers of tables. This is particularly important for enterprise deployments where rule projects may contain hundreds or thousands of individual tables.

**Key Components**

- **Configurable page size**: Use the `size` query parameter to control results per page
- **Total element count**: Responses include total count for client-side pagination controls
- **Unpaged mode**: Available for backwards compatibility or when full results are needed
- **Consistent pagination**: Uniform pagination across all table listing endpoints

**Benefits**

Clients can now efficiently navigate large projects without loading thousands of table definitions at once, improving both client performance and server resource utilization.

**Technical Details**

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

---

#### Case-Insensitive Filtering in Selectors

**Overview**

Selectors now support case-insensitive partial match filtering, improving usability when searching for specific tables or elements within large projects.

**Key Components**

- **Partial matching**: Find tables containing the search term anywhere in the name
- **Case-insensitive**: Searches work regardless of capitalization
- **Name filter parameter**: Consistent filtering parameter across endpoints

**Benefits**

Users can quickly find tables without needing to remember exact names or capitalization, significantly improving the search experience in large projects.

---

### Security

#### OAuth2/OIDC Compliance Improvements

**Overview**

User identity handling has been updated to use the 'sub' claim per OIDC specification, improving interoperability with standards-compliant identity providers.

**Key Components**

- **OIDC-compliant user identity**: Uses 'sub' claim as specified by OpenID Connect
- **Improved provider compatibility**: Better integration with Keycloak, Okta, Azure AD, and other providers
- **Simplified OAuth2 configuration**: Streamlined setup wizards in the admin UI

**Benefits**

Organizations using OAuth2/OIDC providers experience better compatibility and easier integration. The standards-compliant approach ensures interoperability with a wide range of identity providers.

---

#### Spring Security 6 Migration

**Overview**

Security configuration has been migrated from XML-based to Java-based configuration, aligning with Spring Security 6.x best practices and improving maintainability.

**Key Components**

- **Java-based security configuration**: Modern SecurityFilterChain configuration
- **Improved customization**: Easier to extend and customize security rules
- **Better IDE support**: Full IDE support for security configuration

---

### OpenL Repository

#### Enhanced Repository Configuration UI

**Overview**

Repository management has been enhanced with a visual configuration interface that provides real-time validation and connection testing before saving repository settings.

**Key Components**

- **Visual repository configuration**: Intuitive forms for all repository types
- **Connection testing**: Verify connectivity before saving settings
- **Multiple repository types**: Git, database, AWS S3, Azure Blob, and local file system
- **Design and production repositories**: Unified configuration interface

---

### Documentation

#### Academic Citation Support

**Overview**

Added CITATION.cff file for proper academic citation of OpenL Tablets, enabling researchers and academics to properly cite the project in their publications.

---

#### Contribution Guidelines

**Overview**

Added CONTRIBUTING.md with comprehensive project contribution guidelines and Contributor Covenant Code of Conduct for community contributors.

---

## Improvements

### OpenL Core

- **Module consolidation**: Core modules (grammars, core) merged into the rules module for simplified architecture and faster build times
- **Removed JGraphT dependency**: Replaced with simpler built-in implementation, reducing external dependencies
- **Removed Spring dependency from org.openl.rules.project**: Module no longer depends on Spring Framework, enabling lighter deployments
- **ISO-8601 date formatting**: String presentation of Date cell values now uses ISO-8601 format for better internationalization
- **ProjectDescriptor access**: Custom handlers can now access ProjectDescriptor (rules.xml) for advanced customization

### OpenL Studio

- **Project tags persistence**: Tags are now saved to files, surviving restarts and repository operations
- **Administrator status indicator**: User profiles now display a flag indicating administrator status
- **External scripts support**: Settings responses include external scripts configuration for extensibility
- **Case-insensitive selector filtering**: Improved usability when searching for items

### OpenL Rule Services

- **Spring 6.x integration**: Updated validation message handling and OpenAPI configuration for Spring 6 compatibility
- **Improved classloader handling**: Uses correct classloader for Spring Context in complex deployment scenarios
- **Simplified OpenAPI schemas**: Internal endpoints now use @Hidden annotation for cleaner API documentation

### Build and Infrastructure

- **EditorConfig standardization**: Added EditorConfig for consistent coding styles across the project
- **Automated dependency updates**: Configured Dependabot for automated dependency updates across multiple package ecosystems
- **Beta release automation**: GitHub workflow for automated beta releases
- **Improved Docker images**: Updated to use Alpine base image with reduced vulnerabilities
- **Podman support**: Support for podman in rootless mode for containerized deployments

---

## Breaking Changes

### OpenL Core

#### Java 21 Minimum Requirement

**Impact**: Affects all deployments. Applications running on Java 11 or 17 will not start.

**Action Required**:
1. Install JDK 21 or later (recommended: Eclipse Temurin or Amazon Corretto)
2. Update `JAVA_HOME` environment variable
3. Update Docker base images to use Java 21
4. Update CI/CD pipelines to use Java 21
5. Rebuild any custom extensions with JDK 21

---

#### Jakarta EE 10 Namespace Migration

**Impact**: Affects all custom extensions, integrations, and rule interceptors that use Java EE APIs.

**Action Required**:
1. Search your codebase for `import javax.` statements
2. Replace with corresponding `import jakarta.` statements:
   - `javax.servlet.*` to `jakarta.servlet.*`
   - `javax.persistence.*` to `jakarta.persistence.*`
   - `javax.validation.*` to `jakarta.validation.*`
   - `javax.annotation.*` to `jakarta.annotation.*`
3. Update XML namespaces in configuration files
4. Update Maven dependencies to Jakarta EE 10 versions
5. Rebuild and test thoroughly

**Example**:
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

---

#### JSON Serialization Type Format Change

**Impact**: Affects clients expecting CLASS-based type identifiers in JSON responses.

**Action Required**:
1. Update client applications to handle NAME-based type identifiers
2. If you must retain CLASS format (not recommended), set:
   ```properties
   ruleservice.jackson.jsonTypeInfoId=CLASS
   ```
3. Consider migrating stored JSON data to the new format

---

### OpenL Studio

#### Removed CAS SSO Support

**Impact**: Affects organizations using CAS (Central Authentication Service) for single sign-on.

**Action Required**:
1. Evaluate OAuth2/OIDC providers (Keycloak, Okta, Azure AD, etc.)
2. Configure your CAS server to expose an OAuth2/OIDC interface if possible
3. Update OpenL Tablets to use OAuth2/OIDC authentication
4. Test authentication flows thoroughly before production deployment

---

#### Removed Install Wizard

**Impact**: Affects fresh installations that previously used the wizard.

**Action Required**:
1. Configure OpenL Tablets using `application.properties` or environment variables
2. Use the provided configuration templates as a starting point
3. For containerized deployments, use environment variables for configuration

---

### OpenL Studio Admin

#### Removed Deploy Configuration Feature

**Impact**: Affects deployments using the legacy Deploy Configuration UI or endpoints.

**Action Required**:
1. Migrate deployment scripts to use the new Deployment REST API
2. Update CI/CD pipelines to use API-based deployment
3. Use the new React deployment dialog for manual deployments

---

#### Removed Legacy ACL APIs

**Impact**: Affects integrations using deprecated ACL endpoints.

**Action Required**:
1. `/rest/admin/management/groups/settings` - Use new `/api/v1/admin/acl/*`
2. `/rest/admin/management/old/groups` - Use new `/api/v1/admin/groups/*`
3. `/rest/repo/*` - Use new `/api/v1/repositories/*`

---

### OpenL Rule Services

#### Removed RMI Protocol Support

**Impact**: Affects clients using RMI protocol to connect to Rule Services.

**Action Required**:
1. Migrate RMI clients to REST or gRPC
2. Update service configuration to expose REST or gRPC endpoints
3. Update client applications to use the new protocols

---

#### Removed Variations Support

**Impact**: Affects rules using the variations feature.

**Action Required**:
1. Identify rules that use variations
2. Convert variations to parameterized rules or separate rule versions
3. Update calling code to use the new approach
4. Test thoroughly to ensure equivalent behavior

---

### OpenL Repository

#### Removed Cassandra and Hive Support

**Impact**: Affects deployments using Cassandra or Hive as repository storage.

**Action Required**:
1. Export rules from the current repository
2. Set up a new supported repository (Git, database, S3, Azure Blob, etc.)
3. Import rules into the new repository
4. Update configuration to point to the new repository
5. Test rule compilation and execution

---

#### JDBC Drivers No Longer Bundled

**Impact**: Affects deployments using database repositories.

**Action Required**:
1. Download the JDBC driver for your database
2. Place the driver JAR in the classpath:
   - For standalone: `lib/` directory
   - For Docker: Mount as a volume or extend the image
3. Restart OpenL Tablets

---

### DEMO

#### Removed Demo User Mode

**Impact**: Affects deployments using `user.mode=demo` configuration.

**Action Required**:
1. Configure standard authentication for the environment
2. Create test users through the administration interface
3. Update any scripts or automation that relied on demo mode

---

### Security

#### Removed Activity Logging Feature

**Impact**: Affects deployments using built-in activity logging.

**Action Required**:
1. Implement external logging solutions if needed
2. Consider using application server audit logging
3. Configure log aggregation tools for compliance requirements

---

## Bug Fixes

### OpenL Core

- **DoubleRange NaN handling**: Fixed `DoubleRange.contains()` to properly handle NaN values according to IEEE 754 semantics, preventing incorrect range evaluations

### OpenL Studio

- **Method graph for overloaded tables**: Fixed method graph visualization for overloaded decision tables, now correctly displaying all overload variants
- **Multicall method dependencies**: Fixed method graph visualization for multicall method dependencies, showing accurate dependency relationships
- **Project dependencies with database repositories**: Fixed NullPointerException when using project dependencies with non-branch enabled database repositories
- **OAuth2 settings update**: Fixed issue where updating OAuth2 settings required re-entering the Client Secret
- **First repository creation**: Fixed adding the first repository in fresh installations
- **Projects REST API**: Fixed projects REST API request handling
- **Remote repository URI**: Fixed defining remote repository URI in repository configuration
- **Help page navigation**: Fixed direct navigation to the Help page
- **Repository page crash**: Fixed repository page crash when 'repository.name' is configured as read-only
- **AccessDeniedHandler execution**: Fixed unpredictable order of AccessDeniedHandler execution

### Build and Infrastructure

- **Windows 11 build**: Fixed building under Windows 11 with webpack 6.0.0 features
- **Java 25 DEMO package**: Fixed configuration for DEMO package under Java 25
- **Groovy scripts**: Fixed Groovy scripts compatibility with updated dependencies
- **AWS SDK regions**: Fixed test compatibility with new AWS regions added in recent AWS SDK versions

---

## Fixed Vulnerabilities

- **Node.js security update**: Upgraded to Node.js 24.13.0 to address known vulnerabilities
- **Alpine base image**: Updated Docker images to use latest less vulnerable Alpine base image

---

## Updated Libraries

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

## Removed Features Summary

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

## Migration Notes

### Pre-Upgrade Checklist

Before upgrading to OpenL Tablets 6.0.0:

- [ ] Verify Java 21 or later is installed
- [ ] Backup all repositories and databases
- [ ] Document current configuration settings
- [ ] Identify custom extensions and integrations
- [ ] Review removed features and plan alternatives
- [ ] Test upgrade in a non-production environment first

### Java Runtime Migration

1. **Install Java 21**: Download and install JDK 21 (Eclipse Temurin, Amazon Corretto, or Oracle JDK)
2. **Update environment**: Set `JAVA_HOME` to the new JDK path
3. **Update containers**: Use Java 21 base images for Docker deployments
4. **Update build tools**: Ensure Maven/Gradle use JDK 21

### Jakarta EE Migration

If you have custom Java code extending OpenL Tablets:

1. **Find affected imports**: Search for `import javax.servlet`, `import javax.persistence`, etc.
2. **Replace namespaces**: Change `javax.*` to `jakarta.*`
3. **Update dependencies**: Use Jakarta EE 10 compatible versions
4. **Rebuild**: Compile with JDK 21 and the updated dependencies

### Authentication Migration

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

### API Migration

**Legacy API to New API mapping:**

| Old Endpoint | New Endpoint |
|--------------|--------------|
| `/rest/repo/*` | `/api/v1/repositories/*` |
| `/rest/admin/management/groups/settings` | `/api/v1/admin/acl/*` |
| `/rest/admin/management/old/groups` | `/api/v1/admin/groups/*` |
| Deploy Configuration | `/api/v1/deployments/*` |

### Repository Migration

If using Cassandra or Hive:

1. Export rules from the current repository
2. Set up a new supported repository (Git, database, S3, etc.)
3. Import rules into the new repository
4. Update configuration to point to the new repository
5. Test rule compilation and execution

### JDBC Driver Configuration

JDBC drivers are no longer bundled. To use database repositories:

1. Download the JDBC driver for your database
2. Place the driver JAR in the classpath:
   - For standalone: `lib/` directory
   - For Docker: Mount as a volume or extend the image
3. Restart OpenL Tablets

### Post-Upgrade Verification

After upgrading:

1. **Verify startup**: Check logs for errors or warnings
2. **Test authentication**: Verify users can log in
3. **Test rule compilation**: Compile a sample project
4. **Test rule execution**: Execute rules through Rule Services
5. **Test deployment**: Deploy a project to production
6. **Verify integrations**: Test any external integrations

---

## Getting Help

### Documentation

- **User Guide**: https://openl-tablets.org/documentation
- **Configuration Reference**: https://openl-tablets.org/documentation/configuration
- **API Documentation**: https://openl-tablets.org/documentation/api

### Support Channels

- **Community Forum**: https://openl-tablets.org/forum
- **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
- **Commercial Support**: contact@openl-tablets.org

### Live Demo

Try OpenL Tablets online: https://openl-tablets.org/demo

---

## Acknowledgments

This release includes contributions from the OpenL Tablets community. Special thanks to all contributors who submitted bug reports, feature requests, and pull requests.

---

**GitHub Release**: https://github.com/openl-tablets/openl-tablets/releases/tag/6.0.0

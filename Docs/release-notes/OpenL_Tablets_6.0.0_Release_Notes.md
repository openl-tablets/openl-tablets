# OpenL Tablets 6.0.0 Release Notes

**Release Date:** January 2026
**GitHub Release:** https://github.com/openl-tablets/openl-tablets/releases/tag/6.0.0

---

## Improvements

### Core

- Java 21 is now the minimum required version, enabling modern language features and improved performance.
- Java 25 is fully supported for organizations planning to upgrade to the latest JDK.
- Migrated to Jakarta EE 10, replacing javax.* namespaces with jakarta.* throughout the codebase.
- Merged core modules (grammars, core) into the rules module for simplified architecture.
- Simplified InstantiationStrategy hierarchy by merging CommonRulesInstantiationStrategy into SimpleMultiModuleInstantiationStrategy.
- Merged OpenLMethodHandler into OpenLRulesMethodHandler for cleaner method handling.
- Removed dependency on Spring Framework from org.openl.rules.project module.
- Replaced JGraphT dependency with a simpler built-in implementation.
- Use ISO-8601 format for String presentation of Date cell values.

### OpenL Studio

- New React-based Administration UI replacing the legacy JSF/RichFaces interface for users, groups, security, and repository management.
- Implemented WebSocket-based real-time notifications replacing the polling-based REST API.
- New Deployment REST API with React modal dialog for deploying projects.
- New React UI for Merge API operations.
- Added pagination support for Tables API with configurable page size and total element count.
- Implemented case-insensitive partial match filtering in selectors.
- Added ACL import/export API for backing up and restoring access control settings.
- New REST API for repository settings management (design and production repositories).
- New REST API for system properties configuration.
- New Settings API for Security page configuration.
- Added notifications and preloaders for repository editing and deployment process.
- Added external scripts support to settings response.
- Migrated to Spring Security 6 Java-based configuration, removing XML security configuration.
- OAuth2/OIDC improvements: use 'sub' claim as user identity per OIDC specification.
- User profile customization for default user in single-user mode.
- Project tags are now saved to file for persistence.
- Added flag to user profile indicating administrator status.
- Count of group members now displayed in group management.

### Rule Services

- Migrated to Spring 6.x with updated validation message handling and OpenAPI configuration.
- Use correct classloader for Spring Context in complex deployment scenarios.
- Simplified OpenAPI schema generation by using @Hidden annotation for internal endpoints.
- Support accessing ProjectDescriptor (rules.xml) inside custom handlers.

### Build & Infrastructure

- Added EditorConfig for consistent coding styles across the project.
- Configured Dependabot for automated dependency updates across multiple package ecosystems.
- Added CITATION.cff for proper academic citation of OpenL Tablets.
- Added CONTRIBUTING.md with project contribution guidelines.
- Added Contributor Covenant Code of Conduct.
- GitHub workflow for automated beta releases.
- Updated Docker images to use Alpine base image with reduced vulnerabilities.
- Support for podman in rootless mode.

---

## Fixed Bugs

### Core

- Fixed DoubleRange.contains() to properly handle NaN values according to IEEE 754 semantics.

### OpenL Studio

- Fixed method graph visualization for overloaded decision tables.
- Fixed method graph visualization for multicall method dependencies.
- Fixed NullPointerException when using project dependencies with non-branch enabled database repositories.
- Fixed OAuth2 settings update that previously required re-entering Client Secret.
- Fixed adding the first repository in fresh installations.
- Fixed projects REST API request handling.
- Fixed defining remote repository URI in repository configuration.
- Fixed direct navigation to the Help page.
- Fixed repository page crash when 'repository.name' is configured as read-only.
- Fixed unpredictable order of AccessDeniedHandler execution.
- Fixed test compatibility with new AWS regions added in recent AWS SDK versions.

### Build & Infrastructure

- Fixed building under Windows 11 with webpack 6.0.0 features.
- Fixed configuration for DEMO package under Java 25.
- Fixed Groovy scripts compatibility with updated dependencies.

---

## Removed Features

- **CAS SSO Support**: Removed CAS Single Sign-On integration; use OAuth2/OIDC or SAML instead.
- **Variations Support**: Removed rule variations functionality.
- **Deploy Configuration**: Removed legacy Deploy Configuration; use new Deployment API instead.
- **Install Wizard**: Removed the Install Wizard; configure via application.properties or environment variables.
- **RMI Protocol**: Removed RMI protocol support; use REST or gRPC instead.
- **Cassandra Support**: Removed Apache Cassandra repository support.
- **Hive Support**: Removed Apache Hive repository support.
- **Activity Support**: Removed activity logging support.
- **Embedded JDBC Drivers**: Removed bundled JDBC drivers; provide drivers externally.
- **Old ACL API**: Removed deprecated /rest/admin/management/groups/settings and /rest/admin/management/old/groups endpoints.
- **Old Repository API**: Removed deprecated /rest/repo/* API endpoints.
- **User Workspace Home Setting**: Removed 'User Workspace Home' setting from UI.
- **Demo User Mode**: Removed user.mode=demo configuration option.
- **Custom Spreadsheet Type**: Removed custom.spreadsheet.type property support.
- **Dispatching Mode**: Removed dispatching.mode property support.
- **SpreadsheetResult Converters**: Removed legacy SpreadsheetResult converters support.
- **Cacheable Property**: Removed deprecated 'cacheable' property support.
- **Recalculate Property**: Removed deprecated 'recalculate' property support.
- **Old OpenL Assistant Integration**: Removed legacy OpenL Assistant integration.
- **Deprecated Operators**: Removed StringOperators, MulDivNullToOneOperators, and WholeNumberDivideOperators.
- **Version Compatibility < 5.25**: Removed support for project versions older than 5.25.
- **OpenL.properties Configuration**: Removed support for OpenL.properties configuration files.

---

## Migration Notes

- **Java Version**: Upgrade to Java 21 or later before upgrading to OpenL Tablets 6.0.0.
- **Jakarta EE**: Update import statements from javax.* to jakarta.* in custom extensions.
- **Spring Security**: Migrate XML-based security configuration to Java-based configuration.
- **OAuth2**: Update OAuth2 configuration to use OIDC-compliant settings; 'sub' claim is now used for user identity.
- **CAS SSO**: Migrate from CAS to OAuth2/OIDC or SAML authentication.
- **Deploy Configuration**: Use the new Deployment REST API instead of Deploy Configuration.
- **JDBC Drivers**: Provide JDBC drivers externally; they are no longer bundled.
- **Repository API**: Migrate from /rest/repo/* to new repository management APIs.
- **ACL API**: Migrate to new ACL Projects and Repository APIs.
- **SpreadsheetResult**: The ruleservice.jackson.jsonTypeInfoId property now defaults to NAME.

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

## Live Demo

Try OpenL Tablets online: https://openl-tablets.org/demo

---

## Getting Help

- **Documentation**: https://openl-tablets.org/documentation
- **Community Forum**: https://openl-tablets.org/forum
- **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
- **Commercial Support**: contact@openl-tablets.org

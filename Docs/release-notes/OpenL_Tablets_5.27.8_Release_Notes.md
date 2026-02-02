# OpenL Tablets Release Notes v5.27.8

**Release Date:** September 19, 2024

[v5.27.8 on GitHub](https://github.com/openl-tablets/openl-tablets/releases/tag/5.27.8)

## Improvements

### Core

* **Java 21 Build Support**: OpenL Tablets now fully supports building and running under Java 21. All unit and integration tests have been verified on both Java 11 and Java 21, ensuring the rules engine operates correctly on the latest long-term support Java release.

* **SpreadsheetResult to Bean Conversion**: Introduces automatic `valueOf(SpreadsheetResult)` method generation for seamless conversion of SpreadsheetResult objects to their corresponding Java bean representations. This simplifies working with spreadsheet outputs in strongly-typed Java code and improves integration with external systems.

* **Modern Cloning Implementation**: Completely rewrites the object cloning mechanism to operate without illegal reflective access, ensuring full compatibility with Java 9+ module restrictions. The new implementation uses simpler bean introspection techniques instead of complex reflection chains, delivering better performance and future-proofing.

* **Safe Class Loading**: Uses OpenLClassLoader to define generated classes via direct classloader method calls, replacing deprecated Unsafe-based class definition approaches that were blocked in recent Java versions.

* **Memory Optimization for Decision Tables**: Optimizes the equals index implementation to reuse existing search nodes when identical, significantly reducing memory consumption during rule compilation and execution for large decision tables.

* **Improved Reflection Utilities**: Introduces a new utility class for working with Java Bean properties and fields via safe reflection operations, replacing illegal reflective access patterns with JSON deserialization using Jackson for complex object graphs.

* **Modern HTTP Client**: Replaces legacy hacked HttpURLConnection implementations with the JDK 11 HttpClient API, providing better performance, connection pooling, and HTTP/2 support.

### OpenL Studio

* **jQuery 3.7 Migration**: Upgrades the frontend from legacy jQuery 1.x to jQuery 3.7.x with custom overrides for RichFaces compatibility. This modernizes the UI framework, improves browser compatibility, and addresses security vulnerabilities in the older jQuery version.

* **Improved OAuth2 Authentication**: Enhances OAuth2 authentication with graceful error handling when external authentication servers are unavailable. Users now see meaningful error messages instead of internal server error pages when identity providers are temporarily unreachable.

* **Keycloak Integration Testing**: Adds comprehensive Keycloak-based integration tests to validate OAuth2 authentication flows, ensuring reliable authentication across different identity provider configurations.

* **Reduced Database Connections**: Decreases the database connection pool usage to reduce resource consumption under high load, improving stability in multi-user environments and reducing memory footprint.

* **Unlimited Form Fields**: Removes the previous restriction on the number of form fields that can be submitted, allowing complex rule tables with many parameters to be saved without encountering form key limits.

* **Project Validation for API Actions**: Adds validation to ensure projects are properly opened before executing actions that require an active project context via the `/projects` API, preventing confusing error messages from incomplete project states.

* **Product Rebranding**: Updates product naming from "WebStudio" to "OpenL Studio" throughout the user interface for consistent branding and identity.

### Rule Services

* **Local Timezone for JSON Serialization**: Uses the system's local timezone instead of hardcoded UTC for JSON date/time serialization and deserialization. This aligns timestamp handling with user expectations and eliminates timezone conversion confusion in deployed services.

* **Deployment Configuration Improvements**: Refactors Kafka deployment configuration for improved maintainability using patterned file names and property substitution, making it easier to manage multiple deployment configurations.

* **Kafka Configuration Warnings**: Logs warnings when unofficial `kafka-deploy.yml` file names are used, helping administrators identify and correct non-standard configuration files that may cause deployment issues.

* **Pre-Deployment Validation**: Validates services before deployment, catching configuration errors earlier in the deployment pipeline and providing clearer error messages about what needs to be fixed.

* **Deployment YAML in Exports**: Automatically includes the `deployment.yaml` file in downloaded ZIP exports, ensuring complete deployment artifacts are available when exporting projects for deployment.

* **OpenTelemetry 2.0 Compatibility**: Updates the default OpenTelemetry agent port to maintain compatibility with OpenTelemetry 2.0, ensuring observability features continue working after library upgrades.

## Fixed Bugs

### Core

* **REST API Table Updates**: Fixed an issue where update table logic via the REST API could cause incorrect updates under certain race conditions, ensuring reliable table modifications through the API.

### OpenL Studio

* **jQuery Select Value Handling**: Fixed an issue where `$('select').val()` returned incorrect values when the default option was selected and disabled, which caused form submissions to fail in certain scenarios after the jQuery 3 upgrade.

* **Ajax Error Callbacks**: Fixed Ajax error handling to use the `fail` method instead of the deprecated `error` method that was removed in jQuery 3, restoring proper error notification to users.

* **File Upload Component**: Fixed the built-in `fileupload.js` component incompatibility with jQuery 3.x that prevented file uploads from working correctly.

* **Email Input Toggle**: Fixed email input toggle functionality that stopped working after the jQuery upgrade, restoring the ability to enable/disable email notifications.

* **Multiselect Plugin**: Fixed the `changeSelectAllStatus` function for the multiselect plugin to work correctly with jQuery 3, allowing "select all" functionality to operate properly.

* **Position Calculation**: Patched `jQuery.position()` in jQuery 3.7 to use the algorithm from jQuery 1.7.2, fixing incorrect position calculations that returned `{top: 1, left: 1}` and broke UI element positioning.

### Rule Services

* **JAXB and Bean Introspection**: Fixed incompatibility between JAXB deserialization and Java Bean introspection that caused serialization failures when using certain data structures.

* **Jackson MixIn Serialization**: Fixed serialization and deserialization issues when using Jackson MixIn classes for custom JSON mapping configurations.

* **Deployment Version Tag**: Fixed an issue where deployments failed when the `version` tag was defined in `rules-deploy.xml`, ensuring version-tagged deployments work correctly.

## Updated Libraries

| Library | Previous Version | New Version |
|---------|-----------------|-------------|
| Spring Framework | 5.3.36 | 5.3.39 |
| Spring Security | 5.8.12 | 5.8.14 |
| Jackson | 2.17.1 | 2.17.2 |
| Apache CXF | 3.6.3 | 3.6.4 |
| Log4j | 2.23.1 | 2.24.0 |
| SLF4J | 2.0.13 | 2.0.16 |
| Apache Kafka | 3.7.0 | 3.8.0 |
| gRPC | 1.64.0 | 1.66.0 |
| OpenTelemetry | 2.4.0 | 2.8.0 |
| AWS SDK | 2.25.60 | 2.28.2 |
| Azure Storage Blob | 12.26.0 | 12.27.1 |
| Netty | 4.1.110.Final | 4.1.113.Final |
| H2 Database | 2.2.224 | 2.3.232 |
| MS SQL JDBC | 12.6.2.jre11 | 12.8.1.jre11 |
| Apache Cassandra Driver | 4.x | 5.x |
| Swagger Core | 2.2.21 | 2.2.23 |
| Nimbus JOSE JWT | 9.39.1 | 9.41.1 |
| Apache POI | 5.2.5 | 5.3.0 |
| Commons Compress | 1.26.2 | 1.27.1 |
| JUnit | 5.10.2 | 5.11.0 |
| Mockito | 5.12.0 | 5.13.0 |
| TestContainers | 1.19.8 | 1.20.1 |
| Jetty | 10.0.21 | 10.0.24 |
| Maven API | 3.9.7 | 3.9.9 |
| Maven Plugin | 3.13.0 | 3.15.0 |
| Commons Codec | 1.17.0 | 1.17.1 |
| Commons Lang | 3.14.0 | 3.17.0 |
| Guava | 33.2.0-jre | 33.3.0-jre |
| Groovy | 4.0.21 | 4.0.23 |
| Snappy Java | 1.1.10.5 | 1.1.10.7 |
| Byte Buddy | (new) | 1.15.1 |
| jQuery | 1.7.2 | 3.7.1 |

## Removed Features

* **openl2text Module**: The `openl2text` module has been removed from the project, with `OpenL2TextUtils` relocated to a more appropriate location within the codebase. Projects using this utility should update their imports accordingly.

* **Alternative Kafka File Names**: Support for alternative Kafka configuration file names has been removed. Use the standard `kafka-deploy.yml` naming convention for deployment configurations.

* **Legacy Libraries**: Removed dependencies on `picocli` command-line parsing library, `progressbar` library, and `joda-time` library (migrated to the java.time API).

---

For more information, visit [OpenL Tablets Documentation](https://openl-tablets.org/documentation) or [GitHub Repository](https://github.com/openl-tablets/openl-tablets).

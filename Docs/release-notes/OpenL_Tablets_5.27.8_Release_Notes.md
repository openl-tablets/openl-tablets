# OpenL Tablets Release Notes v5.27.8

**Release Date:** September 19, 2024

[v5.27.8 on GitHub](https://github.com/openl-tablets/openl-tablets/releases/tag/5.27.8)

---

## Overview

OpenL Tablets 5.27.8 delivers significant improvements to Java 21 compatibility, modernizes the frontend framework with jQuery 3.7, and introduces a completely rewritten object cloning mechanism that eliminates all illegal reflective access operations. This release also includes important memory optimizations, enhanced OAuth2 authentication handling, and numerous library updates.

---

## New Features

### SpreadsheetResult to Bean Conversion

OpenL Tablets now automatically generates `valueOf(SpreadsheetResult)` methods for spreadsheet-backed Java beans, enabling seamless conversion from SpreadsheetResult objects to their strongly-typed bean representations.

#### Overview

Previously, converting SpreadsheetResult objects to Java beans required manual mapping code or reflection-based utilities. The new automatic generation simplifies integration with external systems and improves type safety.

#### Key Capabilities

* **Automatic Method Generation**: The `valueOf(SpreadsheetResult)` method is generated at compile time for all spreadsheet result beans
* **Multi-Dimensional Array Support**: Includes methods for converting multi-dimension arrays to their bean equivalents
* **Type-Safe Conversion**: Uses compile-time type information to ensure correct field mapping without runtime reflection
* **Common Type Resolution**: New utility methods determine the common type of two classes for improved array handling

#### Technical Details

The implementation extracts spreadsheet field names directly from the spreadsheet definition, eliminating the need for introspection of the generated Java bean. This approach is more efficient and reliable, especially when working with complex nested data structures.

---

## Improvements

### OpenL Core

* **Java 21 Build Support**: OpenL Tablets now fully supports building and running under Java 21. All unit and integration tests have been verified to pass on both Java 11 and Java 21, ensuring the rules engine operates correctly on the latest long-term support Java release. Float rounding behavior differences between Java versions have been addressed in the test suite.

* **Modern Cloning Implementation**: The object cloning mechanism has been completely rewritten to operate without illegal reflective access, ensuring full compatibility with Java 9+ module restrictions. The new implementation uses simpler bean introspection techniques with improved performance, replacing the legacy SafeCloner and CachingArgumentsCloner implementations with a unified, modern approach.

* **Safe Class Definition**: Generated classes are now defined using OpenLClassLoader with direct classloader method calls, replacing deprecated Unsafe-based class definition approaches that were blocked in recent Java versions. This ensures reliable class generation on Java 17+ with strict module enforcement.

* **Memory Optimization for Decision Tables**: The equals index implementation has been optimized to reuse existing search nodes when identical nodes already exist, significantly reducing memory consumption during rule compilation and execution for large decision tables with many indexed conditions.

* **Improved Reflection Utilities**: A new utility class provides safe methods for working with Java Bean properties and fields via reflection. Complex object graphs that previously required illegal reflective access are now handled through JSON deserialization using Jackson, maintaining full functionality while complying with module access restrictions.

* **Modern HTTP Client**: Legacy HttpURLConnection implementations with custom hacks have been replaced with the JDK 11 HttpClient API, providing better performance, automatic connection pooling, HTTP/2 support, and improved timeout handling.

* **Optimized Cloner Performance**: The cloner performance has been improved by using simpler bean introspection instead of complex reflection chains, resulting in faster object copying operations in rule execution scenarios.

### OpenL Studio

* **jQuery 3.7 Migration**: The frontend has been upgraded from the legacy jQuery 1.7.2 to jQuery 3.7.1 with custom overrides maintaining RichFaces compatibility. This modernizes the UI framework foundation, improves browser compatibility with modern browsers, and addresses known security vulnerabilities in the older jQuery version. The migration includes updates to jQuery UI (1.13.2) and Fancytree (2.38.3).

* **Enhanced OAuth2 Authentication**: OAuth2 authentication now gracefully handles scenarios when external authentication servers are unavailable. Users see meaningful error messages explaining the authentication issue instead of generic internal server error pages when identity providers are temporarily unreachable.

* **Keycloak Integration Testing**: Comprehensive Keycloak-based integration tests have been added to validate OAuth2 authentication flows, ensuring reliable authentication across different identity provider configurations and catching regressions in authentication handling.

* **Reduced Database Connections**: Database connection pool usage has been decreased to reduce resource consumption under high concurrent load, improving stability in multi-user environments and reducing the overall memory footprint of the application.

* **Unlimited Form Fields**: The previous restriction on the number of form fields that can be submitted has been removed, allowing complex rule tables with many parameters to be saved without encountering form key limits that previously blocked large table saves.

* **Project Validation for API Actions**: The `/projects` API now validates that projects are properly opened before executing actions that require an active project context, preventing confusing error messages that previously occurred when API calls were made against incomplete project states.

* **Product Rebranding**: The product name has been updated from "WebStudio" to "OpenL Studio" throughout the user interface, documentation, and internal components for consistent branding and improved product identity.

### OpenL Rule Services

* **Local Timezone for JSON Serialization**: JSON date/time serialization and deserialization now uses the system's local timezone instead of hardcoded UTC. This aligns timestamp handling with user expectations and eliminates timezone conversion confusion when services are deployed in non-UTC environments.

* **Deployment Configuration Improvements**: Kafka deployment configuration has been refactored for improved maintainability using patterned file names and property substitution, making it easier to manage multiple deployment configurations in complex environments.

* **Kafka Configuration Warnings**: Warnings are now logged when unofficial `kafka-deploy.yml` file names are used, helping administrators identify and correct non-standard configuration files that may cause deployment issues or be ignored during service startup.

* **Pre-Deployment Validation**: Services are now validated before deployment, catching configuration errors earlier in the deployment pipeline and providing clearer error messages about what needs to be fixed before a service can be deployed successfully.

* **Deployment YAML in Exports**: The `deployment.yaml` file is now automatically included in downloaded ZIP exports, ensuring complete deployment artifacts are available when exporting projects for deployment to other environments.

* **OpenTelemetry 2.0 Compatibility**: The default OpenTelemetry agent port has been updated to maintain compatibility with OpenTelemetry 2.0, ensuring observability features continue working correctly after OpenTelemetry library upgrades.

* **OpenAPI Implementation Without Reflection**: The OpenAPI resource implementation has been rewritten without reflection hacks, using proper ObjectMapper bean configuration from the service context for reliable schema generation.

---

## Fixed Bugs

### OpenL Core

* **REST API Table Updates**: Fixed an issue where update table logic via the REST API could produce incorrect results under certain race conditions, ensuring reliable table modifications through the programmatic API.

### OpenL Studio

* **jQuery Select Value Handling**: Fixed an issue where `$('select').val()` returned incorrect values when the default option was selected and disabled, which caused form submissions to fail in certain scenarios after the jQuery 3 upgrade.

* **Ajax Error Callbacks**: Fixed Ajax error handling to use the `fail` method instead of the deprecated `error` method that was removed in jQuery 3, restoring proper error notification behavior to users when server requests fail.

* **File Upload Component**: Fixed the built-in `fileupload.js` component incompatibility with jQuery 3.x that prevented file uploads from working correctly in the modernized UI.

* **Email Input Toggle**: Fixed email input toggle functionality that stopped working after the jQuery upgrade, restoring the ability to enable and disable email notification settings.

* **Multiselect Plugin**: Fixed the `changeSelectAllStatus` function for the multiselect plugin to work correctly with jQuery 3, allowing "select all" functionality to operate properly in dropdown multi-select components.

* **Position Calculation**: Patched `jQuery.position()` in jQuery 3.7 to use the algorithm from jQuery 1.7.2, fixing incorrect position calculations that returned `{top: 1, left: 1}` instead of correct coordinates and broke UI element positioning throughout the application.

### OpenL Rule Services

* **JAXB and Bean Introspection Compatibility**: Fixed an incompatibility between JAXB deserialization and Java Bean introspection that caused serialization failures when using certain data structures with JAXB annotations.

* **Jackson MixIn Serialization**: Fixed serialization and deserialization issues when using Jackson MixIn classes for custom JSON mapping configurations, ensuring proper handling of polymorphic types and custom serializers.

* **Deployment Version Tag**: Fixed an issue where deployments failed when the `version` tag was defined in `rules-deploy.xml`, ensuring version-tagged deployments work correctly for versioned service deployment scenarios.

---

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
| jQuery UI | (updated) | 1.13.2 |
| Fancytree | (updated) | 2.38.3 |

---

## Removed Features

### openl2text Module

The `openl2text` module has been removed from the project. The `OpenL2TextUtils` class has been relocated to a more appropriate location within the codebase. Projects that imported this utility should update their import statements to reference the new package location.

### Alternative Kafka Configuration File Names

Support for alternative Kafka configuration file names has been removed. Deployments must now use the standard `kafka-deploy.yml` naming convention for Kafka deployment configurations. If you were using alternative file names, rename your configuration files to the standard name.

### Legacy Libraries

The following legacy libraries have been removed as dependencies:

* **picocli**: Command-line parsing library (no longer needed)
* **progressbar**: Progress bar display library (no longer needed)
* **joda-time**: Date/time library (migrated to the standard java.time API)

---

## Build Improvements

* **Multi-threaded Maven Build**: Maven project builds now execute in multi-threaded mode by default, significantly reducing build times on multi-core systems.

* **Java 17 and macOS CI Support**: Continuous integration has been configured to test builds on Java 17 and macOS environments in addition to the standard Linux/Java 11 configuration.

* **Parallelized Integration Tests**: Integration test execution has been reorganized to run tests in parallel where possible, reducing overall CI pipeline duration.

* **Optimized Repository Configuration**: Maven repository configuration has been optimized to avoid attempting snapshot downloads from release repositories, reducing build network overhead.

---

## Migration Notes

### jQuery 3 Compatibility

If you have custom JavaScript code that interacts with the OpenL Studio UI, review your code for jQuery 3 compatibility:

* Replace `.error()` callbacks with `.fail()` for Ajax requests
* Review `$.position()` usage if you rely on element positioning
* Update any plugins that may be incompatible with jQuery 3.x
* Test file upload functionality with your custom extensions

### Timezone Handling

JSON serialization now uses local timezone instead of UTC. If your integration code expects UTC timestamps:

* Update clients to handle local timezone timestamps, or
* Configure timezone explicitly in your deployment environment

### Removed Modules

If your project depends on the removed `openl2text` module, update your Maven dependencies and import statements to use the relocated classes.

---

For more information, visit [OpenL Tablets Documentation](https://openl-tablets.org/documentation) or [GitHub Repository](https://github.com/openl-tablets/openl-tablets).

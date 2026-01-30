# OpenL Tablets 5.27.8 Release Notes

**Release Date:** September 19, 2024
**GitHub Release:** https://github.com/openl-tablets/openl-tablets/releases/tag/5.27.8

---

## Table of Contents

1. [New Features](#new-features)
2. [Improvements](#improvements)
3. [Bug Fixes](#bug-fixes)
4. [Updated Libraries](#updated-libraries)
5. [Deprecated and Removed](#deprecated-and-removed)

---

## New Features

### Java 21 Build Support

OpenL Tablets now fully supports building under Java 21. This enhancement ensures compatibility with the latest Long-Term Support (LTS) version of Java, allowing development teams to leverage the newest JVM features, security improvements, and performance optimizations.

**Key aspects of the Java 21 support:**

- **Build Compatibility**: The entire OpenL Tablets project can now be built using Java 21 as the compiler and runtime environment. All unit tests and integration tests have been verified to pass under both Java 11 and Java 21.

- **Reflection Modernization**: To achieve Java 21 compatibility, extensive refactoring was performed to eliminate illegal reflective access operations that were deprecated in Java 9 and restricted in later versions. The codebase now uses proper public APIs and modern alternatives such as the JDK 11 HttpClient instead of hacked HttpURLConnection, and JSON deserialization instead of reflection-based bytecode generation.

- **Test Adjustments**: Floating-point rounding tests have been updated to accommodate differences in float handling between Java 11 and Java 21, ensuring consistent test results across both platforms.

This change positions OpenL Tablets for long-term compatibility with modern Java environments while maintaining backward compatibility with Java 11.

### SpreadsheetResult to Bean Conversion Enhancement

The spreadsheet processing engine now generates `valueOf(SpreadsheetResult)` methods for seamless conversion of SpreadsheetResult objects to their corresponding Java bean representations. This feature streamlines the process of working with spreadsheet output in Java applications.

**Implementation details:**

- **Automatic Method Generation**: The bytecode generator now creates `valueOf()` static methods that accept SpreadsheetResult parameters and return properly typed bean objects. This eliminates the need for manual conversion code and reduces boilerplate in applications consuming OpenL rules.

- **Multi-dimensional Array Support**: New utility methods have been added to convert multi-dimensional arrays during the bean conversion process. The system can now determine the common type of two classes and perform appropriate conversions automatically.

- **Improved Property Handling**: The conversion now uses spreadsheet field names directly instead of relying on introspection of the generated Java bean. Property naming is now defined on the getters, ensuring Java-compatible identifiers for all properties.

- **OpenAPI Schema Generation**: The OpenAPI schema generation has been updated to properly reflect the new conversion capabilities, ensuring that API documentation accurately describes the available conversion methods.

- **Performance Optimizations**: Key creation has been removed from the runtime path, and the bytecode generator now resolves field duplication issues more efficiently. Unnecessary constructors and methods have been removed from SpreadsheetResult, resulting in cleaner generated code.

---

## Improvements

### OpenL Studio

#### Product Rebranding

The WebStudio application has been officially renamed to **OpenL Studio** throughout the product. This change reflects the unified branding strategy and provides a more consistent identity for the web-based IDE component of OpenL Tablets.

#### OAuth2 Authentication Improvements

The OAuth2 authentication flow has been enhanced to provide better error handling and user experience when external authentication servers are unavailable.

- **Graceful Error Handling**: When the external authentication server (such as Keycloak) is not available, the system now properly redirects users to an appropriate error page instead of displaying an internal server error. This provides clearer feedback about the nature of the authentication failure.

- **Keycloak Test Infrastructure**: The OAuth2 test suite has been expanded with Keycloak-based integration tests, ensuring robust validation of authentication flows against a real identity provider.

- **Projects API Enhancement**: The projects API now includes improved validation and better structured responses, making it easier for clients to work with project data programmatically.

#### Form Submission Enhancement

The form handling system now supports an unlimited number of form keys for submitted forms, removing previous restrictions that could cause issues with complex forms containing many fields.

#### Database Connection Management

The database connection pool management has been optimized to decrease the number of simultaneously opened connections to the database. This improvement reduces resource consumption and improves system stability under high load conditions.

### OpenL Rule Services

#### New Cloner Implementation

A completely new object cloning implementation has been introduced that operates without illegal reflection operations, ensuring compatibility with Java 9+ module system restrictions.

**Overview**

The new cloner replaces the previous implementation (based on the third-party cloning library) with a custom solution designed specifically for OpenL's requirements. This change was necessary because the previous cloner relied heavily on `setAccessible(true)` calls that bypass Java's access control, which is now restricted in modern Java versions.

**Key improvements:**

- **Legal Reflection Only**: The new cloner uses only public APIs and legal reflection operations. It leverages JSON serialization/deserialization through Jackson for complex object graphs where direct copying is not feasible.

- **Performance Optimizations**: The cloner performance has been improved by using simpler bean introspection techniques. Instead of complex reflection chains, the new implementation uses direct property access through generated accessor methods.

- **OpenLClassLoader Integration**: Generated classes are now defined using direct calls to classloader methods through the OpenLClassLoader, avoiding deprecated Unsafe-based class definition techniques.

- **Copy Function Support**: The OpenL `copy()` function now uses the new cloner implementation, ensuring consistent behavior across all cloning operations in the rules engine.

#### Memory Usage Optimization

The equals index implementation has been optimized to reuse existing search nodes when they are identical, significantly reducing memory usage in rule services with complex decision tables. This optimization is particularly beneficial for services with large numbers of rules that share common conditions.

#### Kafka Integration Improvements

The Kafka deployment configuration has been refactored for improved maintainability and clarity.

- **Configuration Simplification**: Kafka configuration handling has been streamlined by removing code duplication, inlining utility classes, and using property substitution to resolve default configurations.

- **Pattern-Based Configuration Files**: Configuration files now use patterned file names, providing a clearer convention for organizing deployment configurations.

- **Deprecation Warning**: The system now logs a warning when unofficial `kafka-deploy.yml` file names are used, encouraging users to follow the standard naming conventions.

- **Spring Configuration Organization**: Spring configurations for Kafka have been reorganized into a more logical structure, separating default configurations from deployment-specific configurations.

#### Date/Time Timezone Handling

The JSON serialization and deserialization now uses the system timezone instead of hardcoded UTC for date and time values. This change ensures that date/time values are handled consistently with the server's locale settings, reducing confusion when working with applications in non-UTC timezones.

#### Deployment Enhancements

- **Deployment YAML in Downloads**: The `deployment.yaml` file is now automatically included in the downloaded ZIP file when exporting projects, making it easier to maintain deployment configurations.

- **Version Tag Fix**: Fixed an issue where deploying projects with a `version` tag defined in `rules-deploy.xml` would fail under certain conditions.

- **Service Validation**: Services are now validated before deployment, catching configuration errors earlier in the deployment process.

#### OpenTelemetry Update

The default OpenTelemetry agent port has been changed to accommodate breaking changes in OpenTelemetry 2.0. This ensures compatibility with the latest OpenTelemetry ecosystem.

### OpenL Core

#### Removal of Illegal Reflective Access

Extensive refactoring has been performed throughout the codebase to eliminate all illegal reflective access operations.

- **StoreLogData Injection**: Data injection into StoreLogData now uses safe reflection operations through proper setter methods.

- **OpenAPI Object Access**: Reading data from OpenAPI objects now uses safe reflection operations instead of field access bypasses.

- **Bean Property Utility**: A new utility class for working with Java Bean properties/fields via reflection has been introduced, centralizing all reflection operations and ensuring they use only legal access patterns.

- **Test Modernization**: All tests have been updated to avoid illegal reflective access, ensuring the test suite runs cleanly under strict Java module system settings.

#### REST API Table Update Fix

Fixed an issue with the update table logic via REST API that could cause incorrect updates under certain conditions.

#### Projects API Validation

Added validation to ensure that projects are properly opened before actions that require an open project are executed through the `/projects` API endpoint.

### OpenL Studio Frontend

#### jQuery 3.7.x Migration

The frontend has been migrated from the legacy jQuery version to jQuery 3.7.x, bringing modern JavaScript capabilities and improved security.

**Migration details:**

- **Custom jQuery Override**: A custom version of jQuery has been configured to override the RichFaces bundled version, allowing the use of jQuery 3.7.x with the legacy JSF components.

- **Compatibility Patches**: Several jQuery plugins and custom code have been patched to work correctly with jQuery 3.x, including fixes for `jQuery.position()` which had behavioral changes in the new version.

---

## Bug Fixes

### OpenL Studio

- **Select Default Value**: Fixed an issue where `$('select').val()` returned incorrect values when the default option was selected and disabled in jQuery 3.

- **Ajax Error Handling**: Fixed Ajax error callbacks to use the `fail` method instead of the deprecated `error` method that was removed in jQuery 3.

- **File Upload**: Fixed issues with the built-in `fileupload.js` component that was incompatible with jQuery 3.x.

- **Email Input Toggle**: Fixed the email inputs toggle functionality that was broken after the jQuery upgrade.

- **Multiselect Plugin**: Fixed the `changeSelectAllStatus` function for the multiselect plugin to work correctly with jQuery 3.

- **jQuery Position**: Patched `jQuery.position()` in jQuery 3.7 to use the algorithm from jQuery 1.7.2 because the new version incorrectly returns `{top: 1, left: 1}` in certain scenarios.

### OpenL Rule Services

- **JAXB Deserialization**: Fixed incompatibility between JAXB deserialization and Java Bean introspection that could cause serialization failures with certain object types.

- **Serialization MixIn**: Fixed serialization and deserialization issues when using Jackson MixIn classes for customizing JSON output.

---

## Updated Libraries

### Core Dependencies

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| Spring Framework | 5.3.36 | 5.3.39 |
| Spring Security | 5.8.12 | 5.8.14 |
| Jackson | 2.17.1 | 2.17.2 |
| Apache CXF | 3.6.3 | 3.6.4 |
| Log4j | 2.23.1 | 2.24.0 |
| SLF4J | 2.0.13 | 2.0.16 |

### Messaging and Integration

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| Apache Kafka | 3.7.0 | 3.8.0 |
| gRPC | 1.64.0 | 1.66.0 |
| OpenTelemetry | 2.4.0 | 2.8.0 |

### Cloud and Storage

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| AWS SDK | 2.25.60 | 2.28.2 |
| Azure Storage Blob | 12.26.0 | 12.27.1 |
| Netty | 4.1.110.Final | 4.1.113.Final |

### Database

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| H2 Database | 2.2.224 | 2.3.232 |
| MS SQL JDBC | 12.6.2.jre11 | 12.8.1.jre11 |
| Apache Cassandra | 4.x | 5.x |

### API and Documentation

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| Swagger Core | 2.2.21 | 2.2.23 |
| Nimbus JOSE JWT | 9.39.1 | 9.41.1 |

### Document Processing

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| Apache POI | 5.2.5 | 5.3.0 |
| Commons Compress | 1.26.2 | 1.27.1 |

### Build and Testing

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| JUnit | 5.10.2 | 5.11.0 |
| Mockito | 5.12.0 | 5.13.0 |
| TestContainers | 1.19.8 | 1.20.1 |
| Jetty | 10.0.21 | 10.0.24 |
| Maven API | 3.9.7 | 3.9.9 |
| Maven Plugin | 3.13.0 | 3.15.0 |

### Utilities

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| Commons Codec | 1.17.0 | 1.17.1 |
| Commons Lang | 3.14.0 | 3.17.0 |
| Guava | 33.2.0-jre | 33.3.0-jre |
| Groovy | 4.0.21 | 4.0.23 |
| Snappy Java | 1.1.10.5 | 1.1.10.7 |
| Byte Buddy | (new) | 1.15.1 |

### Frontend

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| jQuery | 1.x (legacy) | 3.7.x |

---

## Deprecated and Removed

### Removed Modules

- **openl2text**: The `openl2text` module has been removed from the project. The `OpenL2TextUtils` utility class has been moved to a more appropriate location, and the dependency on Jackson libraries for this functionality has been eliminated.

### Removed Dependencies

- **picocli**: The picocli command-line parsing library has been removed.
- **progressbar**: The progressbar library has been removed.
- **joda-time**: The Joda-Time library dependency has been removed (migration to java.time API).

### Deprecated Configuration

- **Alternative Kafka Configuration File Names**: Support for alternative Kafka configuration file names has been removed. Users should use the standard `kafka-deploy.yml` naming convention.

---

## Live Demo

Try OpenL Tablets online: https://openl-tablets.org/demo

---

## Getting Help

- **Documentation**: https://openl-tablets.org/documentation
- **Community Forum**: https://openl-tablets.org/forum
- **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
- **Commercial Support**: contact@openl-tablets.org

---

## Appendix: Ticket References

### New Features
- EPBDS-14339: Support build under Java 21
- EPBDS-14539: Generate valueOf(SpreadsheetResult) method for conversion to a bean

### Improvements
- EPBDS-13895: WebStudio -> OpenL Studio rebranding
- EPBDS-14249: Kafka configuration refactoring
- EPBDS-14251: Implement Cloner to work without illegal reflection operations
- EPBDS-14252: Replace SafeCloner with new Cloner implementation
- EPBDS-14256: Remove usage of illegal reflective access in OpenAPI generation
- EPBDS-14257: Remove illegal reflective access operations
- EPBDS-14261: Improve cloner performance by using simpler introspection
- EPBDS-14329: OAuth2 authentication improvements
- EPBDS-14345: Decrease database connection usage
- EPBDS-14369: Unlimited form keys for form submission
- EPBDS-14373: Projects API validation
- EPBDS-14407: Migrate to jQuery 3.7.x
- EPBDS-14512: Optimize equals index memory usage
- EPBDS-14514: Library updates
- EPBDS-14574: Deployment improvements
- EPBDS-14582: Use system timezone instead of UTC
- EPBDS-14588: Warning for unofficial kafka-deploy.yml

### Bug Fixes
- EPBDS-14416: Fix multiselect plugin changeSelectAllStatus function
- EPBDS-14417: Patch jQuery.position for jQuery 3.7
- EPBDS-14432: Fix select default value behavior in jQuery 3
- EPBDS-14433: Fix fileupload.js compatibility with jQuery 3
- EPBDS-14434: Fix update table logic via REST API
- EPBDS-14437: Fix email inputs toggle
- EPBDS-14438: Fix Ajax error handling for jQuery 3

### Code Cleanup
- EPBDS-13774: Remove openl2text module
- EPBDS-14015: Move OpenL2TextUtils to proper location
- EPBDS-14243: Change default OpenTelemetry port
- EPBDS-9061: Add test for non-reproducing bug
- EPBDS-9436: Add test for getValue error message
- EPBDS-9697: Code simplification
- EPBDS-9821: Add test for null to primitive cast
- EPBDS-10483: Code simplification
- EPBDS-14365: Timezone handling improvements

# OpenL Tablets 5.27.8 Release Notes

**Release Date:** September 2024

---

## Improvements

### Rule Services

- Improved cloner performance by using simpler bean introspection, resulting in faster rule execution
- Optimized equals index to reuse existing search nodes, significantly reducing memory usage for large decision tables
- Generated `valueOf(SpreadsheetResult)` method for efficient conversion of spreadsheet results to beans
- Added support for multi-dimensional array conversion in spreadsheet results
- Improved OpenAPI schema generation for spreadsheet-based services
- Date/time serialization now uses system timezone instead of UTC for better consistency with local environments
- Added deployment.yaml file in downloaded zip files for repositories that don't support folders
- Warning is now shown in logs when unofficial kafka-deploy.yml file name is used
- Kafka configuration simplified with patterned file names and property substitution

### Core

- Support for building and running under Java 21
- Removed illegal reflective access operations for better compatibility with modern Java versions
- New Cloner implementation that works without reflection hacks
- Improved bean property handling with new utility for working with Java Beans via reflection
- Replaced JDK HttpURLConnection with JDK 11 HttpClient for better performance and security
- Removed deprecated openl2text module and moved utilities to proper locations
- Optimized bytecode generation for spreadsheet result beans

### OpenL Studio

- Rebranded from "WebStudio" to "OpenL Studio" across the application
- Migrated to jQuery 3.7.x with full backward compatibility layer
- Fixed multiselect plugin functionality after jQuery upgrade
- Fixed jQuery.position function compatibility with jQuery 3.7
- Improved email input toggle functionality
- Fixed Ajax error handling after jQuery 3 migration
- Fixed file upload component compatibility with jQuery 3.x
- Fixed select default option behavior after jQuery 3 upgrade
- Set unlimited number of form keys for form submissions
- Added validation for project open status before performing actions via REST API
- Improved Projects API with better error handling
- OAuth2 tests setup with Keycloak for better authentication testing
- Decreased the number of opened connections to the database

### Build & Development

- Parallelized integration tests for faster build times
- Multi-threaded Maven project build
- Configured Java 17 and macOS for building
- Updated default OpenTelemetry port due to breaking changes in OTEL 2.0

---

## Bug Fixes

### OpenL Studio

- Fixed redirect to Internal Server Error HTML page when external authentication server is not available
- Fixed update table logic via REST API
- Fixed incompatibility between JAXB deserialization and Java Bean introspection

### Rule Services

- Fixed deploying when 'version' tag is defined in rules-deploy.xml
- Fixed OpenAPI schema generation for complex spreadsheet types
- Fixed serialization and deserialization using MixIn annotations
- Fixed ASM library version conflicts

---

## Updated Libraries

- Spring Framework 5.3.39
- Spring Security 5.8.14
- Nimbus JOSE+JWT 9.41.1
- Byte Buddy 1.15.1
- Apache Kafka 3.8.0
- Snappy Java 1.1.10.7
- OpenTelemetry 2.8.0
- Jackson 2.17.2
- Apache CXF 3.6.4
- gRPC 1.66.0
- Swagger Core 2.2.23
- Log4j 2.24.0
- SLF4J 2.0.16
- Maven API 3.9.9
- Jetty 10.0.24
- Amazon AWS SDK 2.28.2
- Azure Storage Blob 12.27.1
- Netty 4.1.113.Final
- Reactor Netty HTTP 1.1.22
- H2 Database 2.3.232
- MSSQL JDBC 12.8.1.jre11
- JUnit 5.11.0
- Awaitility 4.2.2
- Mockito 5.13.0
- TestContainers 1.20.1
- TestContainers Keycloak 2.6.0
- MinIO 8.5.12
- Commons Codec 1.17.1
- Commons Compress 1.27.1
- Commons Lang3 3.17.0
- Groovy 4.0.23
- Apache POI 5.3.0
- Guava 33.3.0-jre
- jQuery 3.7.1
- jQuery UI 1.13.2
- Fancytree 2.38.3

---

## Migration Notes

- **WebStudio renamed to OpenL Studio**: References to "WebStudio" have been updated throughout the application. Configuration files and documentation now use "OpenL Studio".

- **jQuery 3.7.x upgrade**: The UI has been migrated to jQuery 3.7.x. A compatibility layer has been added to maintain backward compatibility with existing customizations.

- **Timezone handling**: Date/time serialization in REST APIs now uses the system timezone instead of UTC. Review your integrations if you depend on UTC-based date formatting.

- **Java 21 compatibility**: This release includes preparations for Java 21 support, including removal of illegal reflective access. Ensure your environment is compatible with these changes.

---

For a full list of changes, see the [GitHub Releases](https://github.com/openl-tablets/openl-tablets/releases/tag/5.27.8).

---

*Thank you to all contributors and users for making this release possible!*

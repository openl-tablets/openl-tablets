# OpenL Tablets v5.27.8 Release Notes

[GitHub Release](https://github.com/openl-tablets/openl-tablets/releases/tag/5.27.8)

## Improvements

### Core

- Added support for building and running under Java 21, ensuring compatibility with the latest LTS JVM version.
- Improved object cloning performance by replacing Java Bean introspection with a simpler and faster method-based approach.
- Replaced the legacy cloning mechanism with a new implementation that avoids unsafe reflection operations and provides more robust fallback behavior.
- Eliminated illegal reflective access throughout the codebase, ensuring full compliance with Java module system restrictions introduced in Java 17+.
- Optimized memory consumption for decision tables by reusing identical search nodes in the equals index.
- Improved SpreadsheetResult conversion to Java beans with automatic `valueOf(SpreadsheetResult)` method generation, including support for multi-dimensional array conversion.
- Removed the `openl2text` module; its functionality has been moved to OpenL Assistant.

### Rule Services

- Added `deployment.yaml` file to downloaded deployment archives for better deployment traceability.
- Fixed service deployment when the `version` tag is defined in `rules-deploy.xml`, enabling versioned service deployments.
- Changed default date serialization and deserialization to use the system timezone instead of UTC.
- Changed the default OpenTelemetry exporter endpoint port from 4317 to 4318 to align with OpenTelemetry SDK 2.x breaking changes.
- Added a warning in the log when an unofficial Kafka deployment configuration file name is used.
- Simplified Kafka deployment configuration by removing code duplication and using patterned file names.

### OpenL Studio

- Migrated from jQuery 1.7.2 to jQuery 3.7.1, including jQuery UI 1.13.2 and Fancytree 2.38.3, improving frontend performance and security.
- Rebranded all references from "WebStudio" to "OpenL Studio" across the UI, documentation, and configuration.
- Set an unlimited number of form keys for submitted forms, removing previous submission size restrictions.
- Added validation that a project is opened before allowing modification operations via the `/projects` REST API.
- Fixed the update table logic via the REST API.
- Improved error handling when an external OAuth2 authentication server is unavailable, preventing redirect to an Internal Server Error page.
- Reduced the default database connection pool size from 100 to 50, preventing potential exhaustion of database connection limits on shared environments.

## Fixed Bugs

### OpenL Studio

- Fixed `jQuery.position()` returning incorrect values `{top: 1, left: 1}` after the jQuery 3.7 migration.
- Fixed `$('select').val()` behavior when the default option is selected and disabled.
- Fixed AJAX error handling by replacing the removed `error` callback with `fail` in jQuery 3.x.
- Fixed the built-in file upload plugin compatibility with jQuery 3.x.
- Fixed email input toggle functionality.
- Fixed the `changeSelectAllStatus` function in the multiselect plugin.

### Rule Services

- Fixed an incompatibility between JAXB deserialization and Java Bean introspection in the cloner.
- Fixed OpenAPI schema generation for SpreadsheetResult-based types.

## Fixed Vulnerabilities

- CVE-2024-38808 - Spring Framework (SpEL DoS)
- CVE-2024-29736 - Apache CXF (SSRF via WADL stylesheet parameter)
- CVE-2024-6763 - Jetty (URI parsing of invalid authority)
- CVE-2024-28752 - Apache CXF (SSRF via Aegis DataBinding)

## Updated Libraries

Spring Framework 5.3.39, Spring Security 5.8.14, Jackson 2.17.2, Apache CXF 3.6.4, Kafka 3.8.0, Jetty 10.0.24, Netty 4.1.113.Final, Reactor Netty HTTP 1.1.22, Log4j 2.24.0, Slf4j 2.0.16, gRPC 1.66.0, OpenTelemetry 2.8.0, Swagger Core 2.2.23, Nimbus JOSE+JWT 9.41.1, Byte Buddy 1.15.1, ASM 9.7, jQuery 3.7.1, jQuery UI 1.13.2, Fancytree 2.38.3, AWS S3 2.28.2, Azure BLOB Storage 12.27.1, MSSQL JDBC 12.8.1.jre11, H2 2.3.232, Apache POI 5.3.0, Commons Compress 1.27.1, Commons Codec 1.17.1, Commons Lang3 3.17.0, Groovy 4.0.23, Guava 33.3.0-jre, Snappy Java 1.1.10.7, Mockito 5.13.0, JUnit 5.11.0, Testcontainers 1.20.1, Minio 8.5.12

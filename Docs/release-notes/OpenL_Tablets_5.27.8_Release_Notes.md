# OpenL Tablets 5.27.8 Release Notes

**Release Date:** September 19, 2024
**GitHub Release:** https://github.com/openl-tablets/openl-tablets/releases/tag/5.27.8

---

## New Features

### Core

- Adds full support for building OpenL Tablets under Java 21, including all unit and integration tests verified on both Java 11 and Java 21.
- Generates `valueOf(SpreadsheetResult)` methods for seamless conversion of SpreadsheetResult objects to their corresponding Java bean representations.

---

## Improvements

### Core

- Replaces the legacy cloning library with a new implementation that operates without illegal reflective access, ensuring compatibility with Java 9+ module restrictions.
- Improves cloner performance by using simpler bean introspection techniques instead of complex reflection chains.
- Uses OpenLClassLoader to define generated classes via direct classloader method calls, avoiding deprecated Unsafe-based class definition.
- Adds a new utility class for working with Java Bean properties/fields via safe reflection operations.
- Replaces illegal reflective access with JSON deserialization using Jackson for complex object graphs.
- Uses JDK 11 HttpClient instead of hacked HttpURLConnection for HTTP operations.
- Removes usage of `setAccessible(true)` calls throughout the codebase.

### OpenL Studio

- Rebrands WebStudio to **OpenL Studio** throughout the product for consistent identity.
- Migrates frontend from legacy jQuery to jQuery 3.7.x with custom overrides for RichFaces compatibility.
- Improves OAuth2 authentication with graceful error handling when external authentication servers are unavailable.
- Adds Keycloak-based integration tests for OAuth2 authentication validation.
- Supports unlimited form keys for submitted forms, removing previous field count restrictions.
- Decreases database connection pool usage to reduce resource consumption under high load.
- Adds validation to ensure projects are opened before actions requiring an open project via `/projects` API.

### Rule Services

- Optimizes equals index implementation to reuse existing search nodes when identical, significantly reducing memory usage.
- Refactors Kafka deployment configuration for improved maintainability using patterned file names and property substitution.
- Logs warning when unofficial `kafka-deploy.yml` file names are used.
- Uses system timezone instead of hardcoded UTC for JSON date/time serialization and deserialization.
- Includes `deployment.yaml` file in downloaded ZIP exports automatically.
- Validates services before deployment, catching configuration errors earlier.
- Changes default OpenTelemetry agent port for compatibility with OpenTelemetry 2.0.

---

## Fixed Bugs

### Core

- Fixed issue where update table logic via REST API could cause incorrect updates under certain conditions.

### OpenL Studio

- Fixed `$('select').val()` returning incorrect values when the default option was selected and disabled in jQuery 3.
- Fixed Ajax error callbacks to use `fail` method instead of deprecated `error` method removed in jQuery 3.
- Fixed built-in `fileupload.js` component incompatibility with jQuery 3.x.
- Fixed email inputs toggle functionality broken after jQuery upgrade.
- Fixed `changeSelectAllStatus` function for multiselect plugin to work with jQuery 3.
- Patched `jQuery.position()` in jQuery 3.7 to use algorithm from jQuery 1.7.2 due to incorrect `{top: 1, left: 1}` returns.

### Rule Services

- Fixed incompatibility between JAXB deserialization and Java Bean introspection causing serialization failures.
- Fixed serialization and deserialization issues when using Jackson MixIn classes.
- Fixed deploying when `version` tag is defined in `rules-deploy.xml`.

---

## Updated Libraries

| Library | Previous Version | New Version |
|---------|------------------|-------------|
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
| jQuery | 1.x (legacy) | 3.7.x |

---

## Removed Features

- Removes the `openl2text` module from the project, moving `OpenL2TextUtils` to a more appropriate location.
- Removes `picocli` command-line parsing library.
- Removes `progressbar` library.
- Removes `joda-time` library dependency (migrated to java.time API).
- Removes support for alternative Kafka configuration file names; use standard `kafka-deploy.yml` naming convention.

---

## Live Demo

Try OpenL Tablets online: https://openl-tablets.org/demo

---

## Getting Help

- **Documentation**: https://openl-tablets.org/documentation
- **Community Forum**: https://openl-tablets.org/forum
- **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
- **Commercial Support**: contact@openl-tablets.org

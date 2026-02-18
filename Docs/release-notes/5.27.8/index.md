## Release Notes

[v5.27.8](https://github.com/openl-tablets/openl-tablets/releases/tag/5.27.8) on the GitHub

OpenL Tablets 5.27.8 focuses on eliminating illegal reflection operations across multiple components, improving security and forward compatibility with modern JVM versions. The core engine, OpenL Studio, and Rule Services have all been updated to avoid `setAccessible(true)` usage and other reflection hacks.

This release also addresses several important bug fixes in REST API data handling, memory consumption, and OAuth2 configuration, along with significant library updates including Spring Framework 5.3.39, Kafka 3.8.0, and Log4j 2.24.0.

### Improvements

Rule Services:

  * Refactored the Kafka configuration to avoid illegal reflection operations

OpenL Studio:

  * Removed the usage of the cloning library in OpenL Studio
  * Removed vulnerable versions of jQuery from OpenL Studio

Core:

  * Rewritten classloader to support dynamic bytecode loading
  * Reimplemented the copy() function without using illegal reflection operations

OpenL Studio, Rule Services:

  * Removed reflection hacks in the dynamic OpenAPI schema generator

OpenL Studio, Rule Services, Core:

  * Rewritten code to eliminate the use of setAccessible(true)

### Fixed Bugs

OpenL Studio:

  * Fixed an issue where not all data was saved via the REST API if the saved table was smaller than the original and contained merged cells
  * Fixed the /rest/projects/projectId/tables endpoint returning Error 500 after multiple operations like project creation, editing, closing, or opening
  * Fixed the "Trace" and "Run" functionalities failing if the input object had more than 1000 fields
  * Fixed incorrect error messages for bad connections to the OAuth2 Identity Provider (IdP)

OpenL Studio, Rule Services:

  * Fixed incorrect date reading from JSON input when processed via a Groovy class
  * Fixed excessive memory consumption

Rule Services:

  * Fixed an issue where the OpenL Deployer could not find the version in rules-deploy.xml
  * Fixed incorrect alignment of the "Services & Links" and "Start Time" columns in the interface

### Updated Libraries

Runtime Dependencies:

  * Spring Framework 5.3.39
  * Spring Security 5.8.14
  * Nimbus JOSE+JWT 9.41.1
  * Kafka 3.8.0
  * Snappy Java 1.1.10.7
  * OpenTelemetry 2.8.0
  * Jackson Object Mapper 2.17.2
  * GRPC 1.66.0
  * Swagger core 2.2.23
  * Log4j 2.24.0
  * SLF4j 2.0.16
  * Maven Plugin API 3.9.9
  * Maven Plugin Plugin 3.15.0
  * Jetty 10.0.24
  * AWS S3 2.28.2
  * Azure BLOB Client 12.27.1
  * Netty 4.1.113.Final
  * Reactor Netty 1.1.22
  * H2 2.3.232
  * MS SQL JDBC 12.8.1.jre11
  * Apache Commons Codec 1.17.1
  * Apache Commons Compress 1.27.1
  * Apache Commons Lang 3.17.0
  * Apache CXF 3.6.4
  * Groovy 4.0.23
  * POI 5.3.0
  * Guava 33.3.0-jre

Test Dependencies:

  * JUnit 5.11.0
  * Awaitility 4.2.2
  * Mockito 5.13.0
  * Testcontainers 1.20.1
  * Testcontainers Keycloak 2.6.0
  * Minio 8.5.12
  * KeyCloak 25.0
  * Cassandra 5.0
  * Byte buddy 1.15.1

Maven:

  * maven-invoker-plugin 3.8.0
  * maven-project-info-reports-plugin 3.6.2
  * rewrite-maven-plugin 5.36.0

Removed:

  * Joda Time 2.12.7
  * Picolcli 4.7.6
  * Progressbar 0.10.1
  * maven-shade-plugin 3.5.2

### Known Issues

  * When executing test tables in OpenL Studio, a ClassCastException is encountered. Additionally, within RuleServices, a java.lang.IllegalArgumentException is thrown during method calls when the SpreadsheetResult cell type combines elements from a SpreadsheetResult array with a single SpreadsheetResult value.

---
title: OpenL Tablets 5.27.9 Release Notes
date: 2024-11-13
description: Configurable database connection pool size, five bug fixes, and comprehensive library updates.
---

OpenL Tablets **5.27.9** introduces a configurable maximum pool size for database connections and includes five bug fixes alongside comprehensive library updates.

## Improvements

* Added ability to configure the maximum pool size for the database connection pool in OpenL Studio.

## Bug Fixes

* Fixed Demo suite startup script allocating only 2 GB of RAM.
* Fixed OpenAPI schema properties being ordered alphabetically instead of following the original specified order.
* Fixed OpenAPI schema customization failing when the `openapi.json` add-on file includes a `servers` property.
* Fixed `verify` goal failing during parallel Maven builds with JDK 11 when assertions are enabled.
* Fixed OpenL Profiler not working with the Spreadsheet table type.

## Library Updates

### Runtime Dependencies

| Library                | Version        |
|:-----------------------|:---------------|
| Spring Framework       | 6.1.15         |
| Spring Boot            | 3.3.6          |
| Spring Security        | 5.8.15 / 6.3.5 |
| Hibernate ORM          | 6.6.3.Final    |
| Hibernate Validator    | 8.0.1.Final    |
| Nimbus JOSE + JWT      | 9.45           |
| ASM                    | 9.7.1          |
| Kafka Clients          | 3.8.1          |
| OpenTelemetry          | 2.9.0          |
| Jackson                | 2.18.1         |
| CXF                    | 4.0.6          |
| gRPC                   | 1.68.1         |
| Swagger Core           | 2.2.25         |
| Swagger Parser         | 2.1.23         |
| Log4j                  | 2.24.1         |
| Amazon AWS SDK         | 2.29.6         |
| Azure Blob Storage SDK | 12.28.1        |
| Jetty                  | 12.0.15        |
| Netty                  | 4.1.114.Final  |
| Reactor Netty          | 1.1.23         |
| HikariCP               | 6.0.0          |
| Commons IO             | 2.17.0         |
| Bouncy Castle          | 1.79           |
| Velocity               | 2.4.1          |
| Guava                  | 33.3.1-jre     |
| JCache                 | 2.6.1.Final    |
| Plexus Utils           | 4.0.2          |
| Hive JDBC              | 4.0.1          |
| Jakarta Annotation API | 3.0.0          |
| Jakarta Activation     | 2.1.3          |
| Jakarta Servlet API    | 6.0.0          |
| Jakarta WS RS API      | 4.0.0          |
| Jakarta XML Bind       | 4.0.2          |
| Jakarta XML WS API     | 4.0.2          |
| Jaxb Runtime           | 4.0.5          |
| reactive-streams       | **deleted**    |

### UI Dependencies

| Library | Version |
|:--------|:--------|
| RapiDoc | 9.3.8   |

### Test Dependencies

| Library        | Version  |
|:---------------|:---------|
| JUnit          | 5.11.3   |
| JUnit Pioneer  | 2.3.0    |
| Mockito        | 5.14.2   |
| Byte Buddy     | 1.15.9   |
| Testcontainers | 1.20.3   |
| MinIO          | 8.5.13   |

### Build Plugins

| Plugin                              | Version |
|:------------------------------------|:--------|
| Maven Archetype Plugin              | 3.3.0   |
| Maven Release Plugin                | 3.1.1   |
| Maven Deploy Plugin                 | 3.1.3   |
| Maven Clean Plugin                  | 3.4.0   |
| Build Helper Maven Plugin           | 3.6.0   |
| Maven Surefire Plugin               | 3.5.0   |
| Maven JAR Plugin                    | 3.4.2   |
| Maven Project Info Reports Plugin   | 3.7.0   |
| Maven Site Plugin                   | 3.20.0  |
| OWASP Dependency Check Maven Plugin | 10.0.4  |
| OpenRewrite Maven Plugin            | 5.41.0  |
| Buildnumber Maven Plugin            | 3.2.1   |
| Maven Javadoc Plugin                | 3.10.1  |
| Maven Plugin Annotations            | 3.15.1  |

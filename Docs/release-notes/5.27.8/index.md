---
title: OpenL Tablets 5.27.8 Release Notes
date: 2024-09-20
description: Reflection-free internals, classloader rewrite, eight bug fixes,
    known issues with SpreadsheetResult type, and library updates.
---

OpenL Tablets **5.27.8** delivers significant internal improvements eliminating illegal reflection operations, a rewritten classloader, and eight bug fixes.

## Improvements

* Refactored the Kafka configuration to avoid illegal reflection operations.
* Removed the usage of the cloning library in OpenL Studio.
* Removed vulnerable versions of jQuery from OpenL Studio.
* Rewritten classloader to support dynamic bytecode loading.
* Reimplemented the `copy()` function without using illegal reflection operations.
* Removed reflection hacks in the dynamic OpenAPI schema generator.
* Rewritten code to eliminate the use of `setAccessible(true)`.

## Bug Fixes

* Fixed not all data being saved via the REST API when the saved table was smaller than the original and contained merged cells.
* Fixed the `/rest/projects/{projectId}/tables` endpoint returning HTTP 500 after multiple operations such as project creation, editing, closing, or opening.
* Fixed Trace and Run functionalities failing when the input object had more than 1000 fields.
* Fixed incorrect error messages for bad connections to the OAuth2 Identity Provider (IdP) in OpenL Studio.
* Fixed the date being read incorrectly from JSON input when processed via a Groovy class.
* Fixed excessive memory consumption.
* Fixed an issue where OpenL Deployer could not find the version in `rules-deploy.xml`.
* Fixed incorrect alignment of the Services & Links and Start Time columns in the Rule Services interface.

## Known Issues

* When executing test tables in OpenL Studio, a `ClassCastException` is encountered. Additionally, in Rule Services, a `java.lang.IllegalArgumentException` is thrown during method calls when the `SpreadsheetResult` cell type combines elements from a `SpreadsheetResult` array with a single `SpreadsheetResult` value.

## Library Updates

### Runtime Dependencies

| Library                | Version       |
|:-----------------------|:--------------|
| Spring Framework       | 5.3.39        |
| Spring Security        | 5.8.14        |
| Nimbus JOSE + JWT      | 9.41.1        |
| Kafka Clients          | 3.8.0         |
| OpenTelemetry          | 2.8.0         |
| Jackson                | 2.17.2        |
| CXF                    | 3.6.4         |
| gRPC                   | 1.66.0        |
| Swagger Core           | 2.2.23        |
| Log4j                  | 2.24.0        |
| SLF4J                  | 2.0.16        |
| Maven API              | 3.9.9         |
| Amazon AWS SDK         | 2.28.2        |
| Azure Blob Storage SDK | 12.27.1       |
| Jetty                  | 10.0.24       |
| Netty                  | 4.1.113.Final |
| Reactor Netty          | 1.1.22        |
| H2 Database            | 2.3.232       |
| MSSQL Driver           | 12.8.1.jre11  |
| Commons Codec          | 1.17.1        |
| Commons Compress       | 1.27.1        |
| Commons Lang           | 3.17.0        |
| Groovy                 | 4.0.23        |
| POI                    | 5.3.0         |
| Snappy Java            | 1.1.10.7      |
| Guava                  | 33.3.0-jre    |
| Joda Time              | **deleted**   |
| Picocli                | **deleted**   |
| Progressbar            | **deleted**   |

### Test Dependencies

| Library                 | Version |
|:------------------------|:--------|
| JUnit                   | 5.11.0  |
| Mockito                 | 5.13.0  |
| Awaitility              | 4.2.2   |
| Byte Buddy              | 1.15.1  |
| Testcontainers          | 1.20.1  |
| Testcontainers Keycloak | 2.6.0   |
| Keycloak                | 25.0    |
| MinIO                   | 8.5.12  |
| Cassandra               | 5.0     |

### Build Plugins

| Plugin                            | Version     |
|:----------------------------------|:------------|
| Maven Invoker Plugin              | 3.8.0       |
| Maven Project Info Reports Plugin | 3.6.2       |
| OpenRewrite Maven Plugin          | 5.36.0      |
| Maven Plugin Plugin               | 3.15.0      |
| Maven Shade Plugin                | **deleted** |

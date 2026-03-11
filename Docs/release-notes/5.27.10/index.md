---
title: OpenL Tablets 5.27.10 Release Notes
date: 2025-01-17
description: Deployment branch restriction for design repositories, permission inheritance
    removal, commit message on deployment, three bug fixes, and library updates.
---

OpenL Tablets **5.27.10** introduces deployment branch restriction for OpenL Studio design repositories, removes permission inheritance, and includes three bug fixes alongside library updates.

## New Features

### Deployment Branch Restriction

OpenL Studio now supports restricting project deployment to a specific branch. A new **Deployment Branch** setting is available in the design repository configuration with the following options:

* **Any Branch** (default) — deployment is allowed from any branch
* **Main Branch Only** — deployment is restricted to the main branch only

## Improvements

* Removed permission inheritance.
* Optimized memory usage for the trace functionality.
* Added ability to specify a commit message during rule deployment.

## Bug Fixes

* Fixed inability to deploy a project previously deployed from a deleted branch.
* Fixed missing validation for constructors when using literals as input parameters.
* Fixed Out-of-Memory error for requests exceeding 128 KB when `LoggingFeature` is enabled.

## Library Updates

### Runtime Dependencies

| Library                | Version       |
|:-----------------------|:--------------|
| Spring Security        | 5.8.16        |
| Nimbus JOSE + JWT      | 10.0.2        |
| JSON Smart             | 2.5.2         |
| Kafka Clients          | 3.9.0         |
| OpenTelemetry          | 2.13.1        |
| Jackson                | 2.18.2        |
| CXF                    | 3.6.5         |
| gRPC                   | 1.70.0        |
| Swagger Core           | 2.2.28        |
| Swagger Parser         | 2.1.25        |
| Log4j                  | 2.24.3        |
| SLF4J                  | 2.0.17        |
| Amazon AWS SDK         | 2.30.28       |
| Azure Blob Storage SDK | 12.29.0       |
| Netty                  | 4.1.119.Final |
| HikariCP               | 6.2.1         |
| Commons Codec          | 1.18.0        |
| Commons IO             | 2.18.0        |
| Gson                   | 2.12.1        |
| Bouncy Castle          | 1.80          |
| Groovy                 | 4.0.25        |
| POI                    | 5.4.0         |
| Guava                  | 33.4.0-jre    |
| XMLSecurity            | 2.3.5         |
| Cloning                | 1.12.1        |
| stax-ex                | **deleted**   |

### Test Dependencies

| Library            | Version      |
|:-------------------|:-------------|
| JUnit              | 5.12.0       |
| Mockito            | 5.15.2       |
| Byte Buddy         | 1.17.1       |
| Awaitility         | 4.3.0        |
| Datasource Proxy   | 1.10.1       |
| Testcontainers     | 1.20.5       |
| MinIO              | 8.5.17       |
| Oracle OJDBC       | 23.7.0.25.01 |
| PostgreSQL Driver  | 42.7.5       |
| MariaDB Driver     | 2.7.12       |

### Build Plugins

| Plugin                              | Version |
|:------------------------------------|:--------|
| Maven Archetype Plugin              | 3.3.1   |
| License Maven Plugin                | 2.5.0   |
| Maven Surefire Plugin               | 3.5.2   |
| Maven Invoker Plugin                | 3.9.0   |
| Maven Project Info Reports Plugin   | 3.8.0   |
| Maven Site Plugin                   | 3.21.0  |
| OWASP Dependency Check Maven Plugin | 11.1.1  |
| OpenRewrite Maven Plugin            | 5.46.3  |
| Maven Javadoc Plugin                | 3.11.2  |
| AspectJ Maven Plugin                | 1.15.0  |

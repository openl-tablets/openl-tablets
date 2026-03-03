---
title: OpenL Tablets 5.26.13 Release Notes
date: 2023-12-20
description: New Rule Services UI, decimal number serialization in non-scientific notation,
    Accept-Language header support, logo redesign, and multiple bug fixes.
---

OpenL Tablets **5.26.13** is a maintenance release introducing a new Rule Services UI, decimal number serialization improvements, Accept-Language header support, logo and icon redesign, and multiple bug fixes.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

### Rule Services

* New Rule Services UI.
* Serialization of decimal numbers to non-scientific notation in REST responses.
* Integrated `Accept-Language` header support in OpenAPI schema.
* Support for OpenL projects without Excel files inside.
* Optimized logging to capture only OpenL method arguments and result objects, reducing the verbosity of logs.

### WebStudio

* OpenL logo and icons redesign.

### Demo

* Java Security Manager usage in the DEMO is dropped.

## Bug Fixes

### Repository

* Fixed Git Large File Storage (LFS) issues with Microsoft Azure Repositories.
* Fixed Git Large File Storage (LFS) issues with Bitbucket Repositories.

### WebStudio

* Fixed unresponsive user interface issue triggered by text searches using quotation marks.

### Rule Services

* Fixed JDBC connections not being closed properly if an exception occurs during the persist operation.

### Core

* Fixed parsing issues with dates containing invalid characters.

## Library Updates

### Runtime Dependencies

| Library                | Version                  |
|:-----------------------|:-------------------------|
| Spring Framework       | 5.3.31                   |
| Spring Boot            | 2.7.18                   |
| Spring Security        | 5.8.8                    |
| Netty                  | 4.1.101.Final            |
| Reactor Netty          | 1.1.13                   |
| Log4j                  | 2.22.0                   |
| SLF4J                  | 2.0.9                    |
| Jetty                  | 10.0.18                  |
| Amazon AWS SDK         | 1.12.604                 |
| Azure Blob Storage SDK | 12.25.0                  |
| MSSQL Driver           | 12.4.2.jre11             |
| JGit                   | 6.8.0.202311291450-openl |
| HikariCP               | 5.1.0                    |
| Commons Compress       | 1.25.0                   |
| Commons IO             | 2.15.1                   |
| Commons Lang3          | 3.14.0                   |
| BouncyCastle           | 1.77                     |
| Groovy                 | 4.0.16                   |
| POI                    | 5.2.5                    |
| Jackson                | 2.16.0                   |
| Swagger Core           | 2.2.19                   |
| Swagger Parser         | 2.1.19                   |
| Swagger UI             | 5.10.3                   |
| SnakeYAML              | 2.2                      |
| Kotlin StdLib          | 1.9.21                   |
| OkHttp                 | 4.12.0                   |
| Guava                  | 32.1.3-jre               |
| XMLSecurity            | 2.3.4                    |

### Test Dependencies

| Library        | Version |
|:---------------|:--------|
| Mockito        | 5.8.0   |
| Testcontainers | 1.19.3  |
| GreenMail      | 1.6.15  |

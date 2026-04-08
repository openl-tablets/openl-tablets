---
title: OpenL Tablets 5.27.3 Release Notes
date: 2023-12-13
description: New Rule Services UI, non-scientific decimal serialization, redesigned
    OpenL logo and icons, five bug fixes, and library updates.
---

OpenL Tablets **5.27.3** introduces a new Rule Services UI, a more readable decimal number format in REST responses, and a redesigned OpenL logo and icons, alongside five bug fixes and library updates.

## Improvements

* New Rule Services UI.
* Decimal numbers of `Double` and `Float` type are now serialized in non-scientific notation in REST responses (for example, `0.0000031415` instead of `3.1415E-5`).
* OpenL logo and icons redesign.
* Java Security Manager usage in the DEMO is dropped.

## Bug Fixes

* Fixed Git Large File Storage (LFS) issues with Microsoft Azure repositories.
* Fixed Git Large File Storage (LFS) issues with Bitbucket repositories.
* Fixed unresponsive user interface issue triggered by text searches using quotation marks.
* Fixed JDBC connections not being closed properly when an exception occurs during a persist operation.
* Fixed parsing issues with dates containing invalid characters.

## Library Updates

### Runtime Dependencies

| Library                | Version                  |
|:-----------------------|:-------------------------|
| Spring Framework       | 5.3.31                   |
| Spring Boot            | 2.7.18                   |
| Jackson                | 2.16.0                   |
| Swagger Core           | 2.2.19                   |
| Swagger Parser         | 2.1.19                   |
| OpenTelemetry          | 1.32.0                   |
| Log4j                  | 2.22.0                   |
| SLF4J                  | 2.0.9                    |
| Amazon AWS SDK         | 1.12.604                 |
| Azure Blob Storage SDK | 12.25.0                  |
| Jetty                  | 10.0.18                  |
| JGit                   | 6.8.0.202311291450-openl |
| Netty                  | 4.1.101.Final            |
| Reactor Netty          | 1.1.13                   |
| MSSQL Driver           | 12.4.2.jre11             |
| HikariCP               | 5.1.0                    |
| Commons Compress       | 1.25.0                   |
| Commons IO             | 2.15.1                   |
| Commons Lang           | 3.14.0                   |
| Bouncy Castle          | 1.77                     |
| Groovy                 | 4.0.16                   |
| POI                    | 5.2.5                    |
| SnakeYAML              | 2.2                      |

### Test Dependencies

| Library        | Version |
|:---------------|:--------|
| Mockito        | 5.8.0   |
| Testcontainers | 1.19.3  |
| GreenMail      | 1.6.15  |

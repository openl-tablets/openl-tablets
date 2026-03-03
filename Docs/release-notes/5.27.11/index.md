---
title: OpenL Tablets 5.27.11 Release Notes
date: 2025-05-07
description: Export execution results in JSON, optimized decision table performance,
    four bug fixes, and comprehensive library updates.
---

OpenL Tablets **5.27.11** introduces JSON export for execution results, decision table performance optimization, and includes four bug fixes alongside comprehensive library updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

* Added ability to export execution results in JSON format.
* Optimized decision table performance through indexing of simple OR conditions.

## Bug Fixes

* Fixed local Git repository going out of sync after a forced push to the remote commit history.
* Fixed error in the Trace functionality when a Decision Table is invoked from the Action column of a TBasic table.
* Fixed performance issue when managing Git branches.
* Fixed OpenAPI schema generation hanging in Rule Services.

## Library Updates

### Runtime Dependencies

| Library                | Version           |
|:-----------------------|:------------------|
| Spring Framework       | 6.1.18            |
| Spring Boot            | 3.3.11            |
| Spring Security        | 6.3.9             |
| Hibernate ORM          | 6.6.13.Final      |
| Hibernate Validator    | 8.0.2.Final       |
| Nimbus JOSE + JWT      | 10.2              |
| ASM                    | 9.8               |
| Kafka Clients          | 4.0.0             |
| OpenTelemetry          | 2.15.0            |
| Jackson                | 2.19.0            |
| CXF                    | 3.6.6 / 4.1.1     |
| gRPC                   | 1.72.0            |
| Jetty                  | 10.0.25 / 12.0.20 |
| Amazon AWS SDK         | 2.31.35           |
| Azure Blob Storage SDK | 12.30.0           |
| Netty                  | 4.2.0.Final       |
| MSSQL Driver           | 12.10.0           |
| HikariCP               | 6.3.0             |
| Commons Collections    | 4.5.0             |
| Commons IO             | 2.19.0            |
| Commons JXPath         | 1.4.0             |
| Gson                   | 2.13.0            |
| POI                    | 5.4.1             |
| AspectJ                | 1.9.24            |
| Guava                  | 33.4.8-jre        |
| Jakarta Annotation API | 3.0.0             |
| Jakarta Activation     | 2.1.3             |
| Jakarta Servlet API    | 6.0.0             |
| Jakarta WS RS API      | 4.0.0             |
| Jakarta XML Bind       | 4.0.2             |
| Jakarta XML WS API     | 4.0.2             |
| Jaxb Runtime           | 4.0.5             |

### Test Dependencies

| Library        | Version |
|:---------------|:--------|
| JUnit          | 5.12.2  |
| Mockito        | 5.17.0  |
| Byte Buddy     | 1.17.5  |
| Testcontainers | 1.21.0  |

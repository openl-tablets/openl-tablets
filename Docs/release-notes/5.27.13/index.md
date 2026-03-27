---
title: OpenL Tablets 5.27.13 Release Notes
date: 2025-09-05
description: Bug fix for unpredictable AccessDeniedHandler ordering and comprehensive library updates.
---

OpenL Tablets **5.27.13** is a maintenance release that includes a bug fix and comprehensive library updates.

## Bug Fixes

* Fixed unpredictable order of `AccessDeniedHandler`.

## Library Updates

### Runtime Dependencies

| Library                | Version           |
|:-----------------------|:------------------|
| Spring Framework       | 6.2.10            |
| Spring Boot            | 3.5.9             |
| Spring Security        | 6.5.3             |
| Spring Integration     | 6.5.1             |
| Hibernate ORM          | 6.6.28.Final      |
| Hibernate Validator    | 8.0.3.Final       |
| Nimbus JOSE + JWT      | 10.4.2            |
| JSON Smart             | 2.6.0             |
| OpenTelemetry          | 2.19.0            |
| Jackson                | 2.20.0            |
| CXF                    | 3.6.8 / 4.1.3     |
| gRPC                   | 1.75.0            |
| Swagger Core           | 2.2.36            |
| Swagger Parser         | 2.1.33            |
| Jetty                  | 10.0.26 / 12.0.25 |
| Kafka Clients          | 4.1.0             |
| Amazon AWS SDK         | 2.33.3            |
| Azure Blob Storage SDK | 12.31.2           |
| Netty                  | 4.2.5.Final       |
| HikariCP               | 7.0.2             |
| Commons Codec          | 1.19.0            |
| Commons Compress       | 1.28.0            |
| Groovy                 | 4.0.28            |
| Snappy Java            | 1.1.10.8          |
| RichFaces              | 10.0.0-openl      |
| JCodeModel             | 4.0.0             |
| Jakarta Mail           | 2.1.4             |
| Angus Mail             | 2.0.4             |

### Test Dependencies

| Library      | Version      |
|:-------------|:-------------|
| JUnit        | 5.13.4       |
| Byte Buddy   | 1.17.7       |
| Mockito      | 5.19.0       |
| Oracle OJDBC | 23.9.0.25.07 |
| GreenMail    | 2.1.5        |

### Build Plugins

| Plugin                   | Version |
|:-------------------------|:--------|
| OpenRewrite Maven Plugin | 6.17.0  |

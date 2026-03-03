---
title: OpenL Tablets 5.27.15 Release Notes
date: 2025-12-16
description: Added Barbados & Jamaica countries to the runtime context list, Secured User API, and
    Comprehensive library updates for improved security and stability.
---

OpenL Tablets **5.27.15** is a maintenance release for comprehensive library updates for improved security and stability.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

- Added Barbados & Jamaica countries to the runtime context list of supported locales.

## Bug Fixes

- Secured User API to prevent reading user information without proper permissions, ensuring that only authorized users can access sensitive data.

## Library Updates

### Runtime Dependencies

| Library                | Version      |
|:-----------------------|:-------------|
| Spring Framework       | 6.2.11       |
| Spring Boot            | 3.5.6        |
| Spring Security        | 6.5.5        |
| Spring Integration     | 6.5.2        |
| Hibernate ORM          | 6.6.31.Final |
| OpenSAML               | 5.1.6        |
| Nimbus JOSE + JWT      | 10.6         |
| ASM                    | 9.9          |
| Kafka Clients          | 4.1.0        |
| OpenTelemetry          | 2.21.0       |
| Jackson                | 2.20.1       |
| gRPC                   | 1.76.0       |
| Swagger Core           | 2.2.40       |
| Swagger Parser         | 2.1.35       |
| Amazon AWS SDK         | 2.38.2       |
| Azure Blob Storage SDK | 12.32.0      |
| Netty                  | 4.2.7.Final  |
| Commons Codec          | 1.20.0       |
| Commons IO             | 2.21.0       |
| Groovy                 | 4.0.29       |
| AspectJ                | 1.9.25       |
| Jakarta Mail           | 2.1.5        |
| Jakarta XML Bind       | 4.0.4        |
| Angus Mail             | 2.0.5        |
| Jakarta Faces          | 4.0.12       |
| Jaxb Runtime           | 4.0.6        |
| MSSQL Driver           | 13.2.1.jre11 |

### Test Dependencies

| Library           | Version     |
|:------------------|:------------|
| JUnit             | 5.14.1      |
| Byte Buddy        | 1.18.0      |
| XMLUnit           | 2.11.0      |
| Testcontainers    | 2.0.1       |
| MinIO             | 8.6.0       |
| PostgreSQL Driver | 42.7.8      |
| Oracle OJDBC      | 23.26.0.0.0 |
| GreenMail         | 2.1.6       |
| Jetty             | 12.1.1      |

### Build Plugins

| Plugin                              | Version |
|:------------------------------------|:--------|
| Maven Archetype Plugin              | 3.4.1   |
| Maven Release Plugin                | 3.2.0   |
| Maven WAR Plugin                    | 3.5.0   |
| OWASP Dependency Check Maven Plugin | 12.1.8  |
| OpenRewrite Maven Plugin            | 6.23.0  |
| JaCoCo Maven Plugin                 | 0.8.14  |
| Maven Javadoc Plugin                | 3.12.0  |

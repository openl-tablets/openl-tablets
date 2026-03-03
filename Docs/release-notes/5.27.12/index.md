---
title: OpenL Tablets 5.27.12 Release Notes
date: 2025-07-23
description: Java 25 support, Jakarta EE 10 compliant OpenL Studio, four bug fixes, and comprehensive library updates.
---

OpenL Tablets **5.27.12** introduces Java 25 support, a Jakarta EE 10 compliant version of OpenL Studio, and includes four bug fixes alongside comprehensive library updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

* Java 25 support.
* Jakarta EE 10 compliant version of OpenL Studio.

## Bug Fixes

* Fixed inconsistent behavior of `DoubleRange.contains(Double.NaN)`.
* Fixed NPE when using dependencies in non-branch-enabled repositories.
* Fixed display issues for tables containing business dimensional properties on the Table Dependencies graph.
* Fixed disconnected tables on the dependencies graph when one table calls another using Multi Call.

## Library Updates

### Runtime Dependencies

| Library                | Version                  |
|:-----------------------|:-------------------------|
| Spring Framework       | 6.2.5                    |
| Spring Boot            | 3.4.5                    |
| Spring Security        | 6.5.1                    |
| Spring Integration     | 6.5.0                    |
| Hibernate ORM          | 6.6.21.Final             |
| Hibernate Validator    | 8.0.2.Final              |
| OpenSAML               | 5.1.4                    |
| Nimbus JOSE + JWT      | 10.3.1                   |
| OpenTelemetry          | 2.18.0                   |
| Jackson                | 2.19.2                   |
| CXF                    | 3.6.7 / 4.1.2            |
| gRPC                   | 1.73.0                   |
| Log4j                  | 2.25.1                   |
| Maven API              | 3.9.11                   |
| Amazon AWS SDK         | 2.32.4                   |
| Azure Blob Storage SDK | 12.31.0                  |
| Jetty                  | 12.0.23                  |
| JGit                   | 7.3.0.202506031305-openl |
| Netty                  | 4.2.3.Final              |
| MSSQL Driver           | 12.10.1                  |
| Commons IO             | 2.20.0                   |
| Commons Lang           | 3.18.0                   |
| Bouncy Castle          | 1.81                     |
| Groovy                 | 4.0.27                   |
| RichFaces              | 10.0.0-openl             |
| XMLSecurity            | 4.0.4                    |
| Jakarta Annotation API | 3.0.0                    |
| Jakarta Activation     | 2.1.3                    |
| Jakarta Mail           | 2.1.3                    |
| Jakarta Servlet API    | 6.0.0                    |
| Jakarta WS RS API      | 4.0.0                    |
| Jakarta XML Bind       | 4.0.2                    |
| Jakarta XML WS API     | 4.0.2                    |
| Jaxb Runtime           | 4.0.5                    |
| Angus Mail             | 2.0.3                    |

### Test Dependencies

| Library           | Version      |
|:------------------|:-------------|
| JUnit             | 5.13.3       |
| Byte Buddy        | 1.17.6       |
| Mockito           | 5.18.0       |
| Datasource Proxy  | 1.11.0       |
| XMLUnit           | 2.10.3       |
| Testcontainers    | 1.21.3       |
| Oracle OJDBC      | 23.8.0.25.04 |
| PostgreSQL Driver | 42.7.7       |
| GreenMail         | 2.1.4        |

### Build Plugins

| Plugin                              | Version |
|:------------------------------------|:--------|
| Maven Archetype Plugin              | 3.4.0   |
| JavaCC Maven Plugin                 | 3.1.1   |
| Maven Deploy Plugin                 | 3.1.4   |
| License Maven Plugin                | 2.6.0   |
| Maven Clean Plugin                  | 3.5.0   |
| Build Helper Maven Plugin           | 3.6.1   |
| Maven Compiler Plugin               | 3.14.0  |
| Maven Surefire Plugin               | 3.5.3   |
| Maven Project Info Reports Plugin   | 3.9.0   |
| OWASP Dependency Check Maven Plugin | 12.1.3  |
| OpenRewrite Maven Plugin            | 6.13.0  |

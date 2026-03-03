---
title: OpenL Tablets 5.26.14 Release Notes
date: 2024-03-06
description: Docker support for MinIO as S3-compatible storage, ISO 8601 date format in toDate() function,
    bug fixes, and library updates.
---

OpenL Tablets **5.26.14** is a maintenance release introducing Docker support for MinIO as an S3-compatible storage repository, ISO 8601 date format support, bug fixes, and library updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

### Rule Services

* Implemented Docker support for MinIO as an S3-compatible storage repository.

### Core

* Added support for the `yyyy-MM-dd` format in the `toDate()` function, adhering to the ISO 8601 standard.

## Bug Fixes

### WebStudio

* Fixed favicon not being displayed in the browser on the Repository tab.

### Rule Services

* Fixed Rule Services frontend failing when any input parameter is null.
* Fixed Rule Service failing to start in the absence of defined publishers.

## Library Updates

### Runtime Dependencies

| Library                | Version       |
|:-----------------------|:--------------|
| Spring Framework       | 5.3.32        |
| Spring Security        | 5.8.10        |
| Jose4j                 | 0.9.5         |
| Kafka Clients          | 3.7.0         |
| Netty                  | 4.1.107.Final |
| Reactor Netty          | 1.1.16        |
| Kotlin StdLib          | 1.9.22        |
| Jackson                | 2.16.1        |
| Swagger Core           | 2.2.20        |
| Swagger Parser         | 2.1.20        |
| Swagger UI             | 5.11.8        |
| Log4j                  | 2.23.0        |
| SLF4J                  | 2.0.12        |
| Jetty                  | 10.0.20       |
| Okio                   | 3.8.0         |
| Amazon AWS SDK         | 2.25.1        |
| Azure Blob Storage SDK | 12.25.2       |
| MSSQL Driver           | 12.6.1.jre11  |
| Commons Codec          | 1.16.1        |
| Commons Compress       | 1.26.0        |
| Groovy                 | 4.0.19        |
| AspectJ                | 1.9.21.1      |
| Guava                  | 33.0.0-jre    |
| Joda Time              | 2.12.7        |

### Test Dependencies

| Library        | Version |
|:---------------|:--------|
| Mockito        | 5.11.0  |
| Testcontainers | 1.19.7  |
| MinIO          | 8.5.9   |

### Build Plugins

| Plugin              | Version |
|:--------------------|:--------|
| Maven Plugin API    | 3.9.6   |
| Maven Plugin Plugin | 3.11.0  |

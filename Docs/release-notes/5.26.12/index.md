---
title: OpenL Tablets 5.26.12 Release Notes
date: 2023-10-18
description: Increased cell display limit, parameter assignment in old constructor syntax, Spring annotation
    support in service interceptors, and library updates.
---

OpenL Tablets **5.26.12** is a maintenance release with increased cell display limits, enhanced Maven plugin error messages, constructor syntax improvements, and library updates.

## Improvements

### WebStudio

* Increased the display limit for cell counts; configurable via the `experimental.MAX_NUM_CELLS` property (default: 20,000).

### Maven Plugin

* Enhanced clarity of error messages for the Maven plugin.

### Core

* Added support for parameter assignment in the old constructor syntax.

  ```java
  new CustomType(param1 = "value1", param2 = "value2")
  ```

### Rule Services

* Supported inherited types in the service annotation template.
* Added support for Spring annotations in the service interceptors.

## Bug Fixes

### Core

* Fixed an issue where External Return did not match properly when input and output objects had the same name but different capitalization with constructor syntax using parameter names.

### WebStudio

* Fixed empty white screen presented after login following logout, or "HTTP ERROR 404 Not Found" errors.

## Library Updates

### Runtime Dependencies

| Library                | Version       |
|:-----------------------|:--------------|
| Spring Framework       | 5.3.30        |
| Spring Boot            | 2.7.16        |
| Spring Security        | 5.8.7         |
| ASM                    | 9.6           |
| Kafka Clients          | 3.6.0         |
| Snappy Java            | 1.1.10.5      |
| Avro                   | 1.11.3        |
| ZooKeeper              | 3.9.1         |
| Thrift                 | 0.19.0        |
| Ant                    | 1.10.14       |
| Netty                  | 4.1.100.Final |
| Reactor Netty          | 1.1.11        |
| Kotlin StdLib          | 1.9.10        |
| CXF                    | 3.6.2         |
| Jetty                  | 10.0.17       |
| Okio                   | 3.6.0         |
| Amazon AWS SDK         | 1.12.565      |
| Azure Blob Storage SDK | 12.24.0       |
| H2 Database            | 2.2.224       |
| MSSQL Driver           | 12.4.1.jre11  |
| Commons Compress       | 1.24.0        |
| Commons IO             | 2.14.0        |
| Groovy                 | 4.0.15        |
| AspectJ                | 1.9.20.1      |
| POI                    | 5.2.4         |

### Test Dependencies

| Library        | Version |
|:---------------|:--------|
| Mockito        | 5.6.0   |
| Testcontainers | 1.19.1  |
| JMH            | 1.37    |

### Build Plugins

| Plugin           | Version |
|:-----------------|:--------|
| Maven Plugin API | 3.9.5   |

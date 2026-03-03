---
title: OpenL Tablets 5.27.1 Release Notes
date: 2023-10-27
description: Increased cell display limit, enhanced Maven plugin error messages,
    constructor and annotation improvements, one bug fix, and library updates.
---

OpenL Tablets **5.27.1** introduces configurable cell display limits, improved Maven plugin error messages, constructor and annotation enhancements, and removes support for previously deprecated properties, alongside one bug fix and library updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

* Increased the display limit for cell counts; configurable via the `experimental.MAX_NUM_CELLS` property (default: 20,000).
* Enhanced clarity of error messages for the Maven plugin.
* Added support for parameter assignment in the old constructor syntax.
* Added support for inherited types in service annotation templates.
* Added support for Spring annotations in service interceptors.
* Removed support for previously deprecated properties: `ruleservice.instantiation.strategy.lazy` and `ruleservice.jaxrs.responseStatusAlwaysOK`.

## Bug Fixes

* Fixed an issue where External Return did not match properly when the input and output objects had the same name but different capitalization.

## Library Updates

### Runtime Dependencies

| Library                | Version       |
|:-----------------------|:--------------|
| Spring Framework       | 5.3.30        |
| Spring Boot            | 2.7.17        |
| Spring Security        | 5.8.8         |
| ASM                    | 9.6           |
| Kafka Clients          | 3.6.0         |
| OpenTelemetry          | 1.31.0        |
| Log4j                  | 2.21.0        |
| CXF                    | 3.6.2         |
| Amazon AWS SDK         | 1.12.572      |
| Azure Blob Storage SDK | 12.24.0       |
| Jetty                  | 10.0.17       |
| Netty                  | 4.1.100.Final |
| Reactor Netty          | 1.1.12        |
| H2 Database            | 2.2.224       |
| MSSQL Driver           | 12.4.1.jre11  |
| Commons Compress       | 1.24.0        |
| Commons IO             | 2.14.0        |
| Groovy                 | 4.0.15        |
| AspectJ                | 1.9.20.1      |
| POI                    | 5.2.4         |
| Guava                  | 32.1.3-jre    |
| XMLSecurity            | 2.3.4         |
| Snappy Java            | 1.1.10.5      |
| Avro                   | 1.11.3        |
| ZooKeeper              | 3.9.1         |
| Thrift                 | 0.19.0        |
| Ant                    | 1.10.14       |
| Kotlin StdLib          | 1.9.10        |
| Okio                   | 3.6.0         |

### Test Dependencies

| Library        | Version |
|:---------------|:--------|
| Mockito        | 5.6.0   |
| Testcontainers | 1.19.1  |
| JMH            | 1.37    |

### Build Plugins

| Plugin    | Version |
|:----------|:--------|
| Maven API | 3.9.5   |

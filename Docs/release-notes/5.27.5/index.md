---
title: OpenL Tablets 5.27.5 Release Notes
date: 2024-03-08
description: MinIO Docker support, ISO 8601 date format in toDate(), REST endpoints for
    rule table modification, project search in OpenL Studio, and four bug fixes.
---

OpenL Tablets **5.27.5** introduces MinIO Docker support, ISO 8601 date format handling, REST endpoints for rule table editing, and a project search feature, alongside four bug fixes and library updates.

## Improvements

* Implemented Docker support for MinIO as an S3-compatible storage repository.
* Added support for the `yyyy-MM-dd` format in the `toDate()` function, adhering to the ISO 8601 standard.
* Implemented RESTful endpoints for modifying rule tables within projects.
* Implemented a search functionality on the Project page in OpenL Studio Editor.

## Bug Fixes

* Fixed users being unable to interact with OpenL Studio Editor after session expiry.
* Fixed a user with only CREATE permissions being unable to view a project they created when the project is derived from a template.
* Fixed Rule Services frontend failing when any input parameter is `null`.
* Fixed Rule Services failing to start when no publishers are defined.

## Library Updates

### Runtime Dependencies

| Library                | Version       |
|:-----------------------|:--------------|
| Spring Framework       | 5.3.32        |
| Spring Security        | 5.8.10        |
| Jose4j                 | 0.9.5         |
| Kafka Clients          | 3.7.0         |
| OpenTelemetry          | 2.1.0         |
| Log4j                  | 2.23.0        |
| SLF4J                  | 2.0.12        |
| Amazon AWS SDK         | 2.25.1        |
| Azure Blob Storage SDK | 12.25.2       |
| Jetty                  | 10.0.20       |
| Netty                  | 4.1.107.Final |
| Reactor Netty          | 1.1.16        |
| MSSQL Driver           | 12.6.1.jre11  |
| Commons Codec          | 1.16.1        |
| Commons Compress       | 1.26.0        |
| Groovy                 | 4.0.19        |
| AspectJ                | 1.9.21.1      |
| Joda Time              | 2.12.7        |

### Test Dependencies

| Library        | Version |
|:---------------|:--------|
| Mockito        | 5.11.0  |
| Testcontainers | 1.19.7  |
| MinIO          | 8.5.9   |

---
title: OpenL Tablets 5.27.6 Release Notes
date: 2024-04-16
description: JSON date format standardization, rule invocation via JSON string arguments,
    four bug fixes, and library updates.
---

OpenL Tablets **5.27.6** introduces a standardized JSON date format for `JsonUtils` and a new way to invoke OpenL rules via JSON string arguments, alongside four bug fixes and library updates.

## Improvements

* `JsonUtils` is set to use the `yyyy-MM-dd'T'HH:mm:ss.SSS` date format by default.
* Introduced the ability to invoke OpenL rules by providing arguments in a JSON string format similar to that used in REST services.

## Bug Fixes

* Fixed HTTP 500 returned by the REST API in OpenL Studio during an asynchronous request when the authentication method is OAuth.
* Fixed selected branches, except for `master` or `main`, disappearing from the branch dropdown list after re-logging in or restarting OpenL Studio.
* Fixed the selected Production repository on the Auto Deploy screen being reset when a Deploy Configuration is selected.
* Fixed OpenL Studio writing data into the public schema when the user database is PostgreSQL.

## Known Issues

* OpenL Studio is not compatible with MS SQL Server during user login, generating an error: *Operand type clash: datetime2 is incompatible with timestamp.* To resolve this, run the following SQL command:

```sql
ALTER TABLE OPENL_LOCK ALTER COLUMN CREATED_DATE DATETIME NOT NULL;
```

## Library Updates

### Runtime Dependencies

| Library                | Version       |
|:-----------------------|:--------------|
| Spring Framework       | 5.3.34        |
| Spring Security        | 5.8.11        |
| Nimbus JOSE + JWT      | 9.37.3        |
| Jose4j                 | 0.9.6         |
| JSON Smart             | 2.5.1         |
| ASM                    | 9.7           |
| OpenTelemetry          | 2.3.0         |
| Jackson                | 2.17.0        |
| CXF                    | 3.6.3         |
| gRPC                   | 1.63.0        |
| Swagger Core           | 2.2.21        |
| Swagger Parser         | 2.1.21        |
| Log4j                  | 2.23.1        |
| SLF4J                  | 2.0.13        |
| Amazon AWS SDK         | 2.25.31       |
| Azure Blob Storage SDK | 12.25.3       |
| Netty                  | 4.1.108.Final |
| Reactor Netty          | 1.1.18        |
| Commons Compress       | 1.26.1        |
| Commons IO             | 2.16.1        |
| Bouncy Castle          | 1.78          |
| Groovy                 | 4.0.21        |
| AspectJ                | 1.9.22        |
| Guava                  | 33.1.0-jre    |
| Picocli                | 4.7.5         |
| Progressbar            | 0.10.1        |
| ZooKeeper              | 3.9.2         |
| Thrift                 | 0.20.0        |

### Test Dependencies

| Library    | Version |
|:-----------|:--------|
| Awaitility | 4.2.1   |

### Build Plugins

| Plugin              | Version |
|:--------------------|:--------|
| Maven Plugin Plugin | 3.12.0  |
| Plexus Utils        | 4.0.1   |

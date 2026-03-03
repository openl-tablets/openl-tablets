---
title: OpenL Tablets 5.27.4 Release Notes
date: 2024-01-24
description: MinIO Docker support in Rule Services, AWS SDK v2 migration, non-versioned S3
    repository support, three bug fixes, and library updates.
---

OpenL Tablets **5.27.4** delivers significant Rule Services improvements including MinIO Docker support, AWS SDK v2 migration, non-versioned S3 repository support, and overall deployment performance improvements, alongside three bug fixes.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Known Issues](#known-issues)
* [Library Updates](#library-updates)

## Improvements

* Implemented Docker support for MinIO as an S3-compatible storage repository in Rule Services.
* Migrated the S3 repository to AWS SDK v2.
* Added support for force-deploying JARs from Rule Services via `ruleservice.datasource.deploy.classpath.jars=always`.
* Added support for non-versioned S3 repositories.
* Added support for `http://minio:9000/` style URLs.
* Improved overall performance of deployment repositories.
* Added support for OpenL projects without Excel files.

## Bug Fixes

* Fixed favicon not being displayed in the browser on the Repository tab.
* Fixed REST branch creation failing for a project when using OAuth2 authentication.
* Fixed expression reference failing with a large number of conditions (exceeding 15).

## Known Issues

* Users may experience intermittent hang-ups in the OpenL Studio interface when performing create, edit, or delete operations on the Editor or Repository tabs. **Workaround:** Clear browser cookies for OpenL Studio and restart the browser.

## Library Updates

### Runtime Dependencies

| Library                | Version       |
|:-----------------------|:--------------|
| Spring Security        | 5.8.9         |
| Jose4j                 | 0.9.4         |
| Kafka Clients          | 3.6.1         |
| OpenTelemetry          | 2.0.0         |
| Jackson                | 2.16.1        |
| Swagger Core           | 2.2.20        |
| Swagger Parser         | 2.1.20        |
| Log4j                  | 2.22.1        |
| SLF4J                  | 2.0.11        |
| Amazon AWS SDK         | 2.23.7        |
| Azure Blob Storage SDK | 12.25.1       |
| Jetty                  | 10.0.19       |
| Netty                  | 4.1.106.Final |
| Reactor Netty          | 1.1.15        |
| Datasource Proxy       | 1.10          |
| Groovy                 | 4.0.18        |
| AspectJ                | 1.9.21        |
| Guava                  | 33.0.0-jre    |
| Joda Time              | 2.12.6        |

### Test Dependencies

| Library | Version |
|:--------|:--------|
| Mockito | 5.9.0   |

### Build Plugins

| Plugin              | Version |
|:--------------------|:--------|
| Maven API           | 3.9.6   |
| Maven Plugin Plugin | 3.11.0  |

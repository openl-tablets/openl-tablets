---
title: OpenL Tablets 5.25.1 Release Notes
date: 2021-10-27
description: Added protected branches, SQL database storage in Rule Services, improved Docker base image, fixed 25 bugs across Rule Services, WebStudio, Maven plugin, core engine, and demo, and updated libraries.
---

OpenL Tablets **5.25.1** is a maintenance release that introduces protected branches functionality, adds SQL database
data storage support in Rule Services, improves the Docker base image, resolves 25 bugs across multiple components, and
updates libraries.

## New Features

### WebStudio

* Added **protected branches** functionality to prevent pushing changes into main or release branches. This can be
  configured via the Admin tab or the `repository.design.protected-branches` property.

## Improvements

### Docker

* Updated to Ubuntu Focal base image and updated JRE implementation to reduce the number of known vulnerabilities.

### WebStudio

* Test cases are now deselected by default in the Trace menu for test tables.

### Rule Services

* Added support for SQL database data storage.

## Bug Fixes

### Rule Services

* Fixed increased memory consumption in Rule Services.
* Fixed exceptions from `StoreLogDataAdvice` being silently ignored.
* Fixed library incompatibility for Jaeger + JBoss 7.3 + Java 11.

### WebStudio

* Fixed the project list being cleared when an entered comment fails the validation pattern.
* Fixed the "Hide deleted projects" filter not being applied correctly when grouping is active.
* Fixed browser console errors occurring during branch synchronization while compilation is in progress.
* Fixed an error being logged when closing or deleting projects after export.
* Fixed the "WebStudio can't open two projects with the same name" error occurring when opening a deployment
  configuration revision.
* Fixed new tag values not being saved when pressing Enter in the "Create project" window for optional extensible tags.
* Fixed an Excel tab with non-alphabetical symbols preventing table editing.
* Fixed the "Project is not found" error occurring on branch updates that delete the current module.
* Fixed the Admin tab UI displaying incorrectly with lengthy repository names.
* Fixed the revisions list disappearing when opening a project with the Revisions tab active.
* Fixed new fields appearing in the Admin tab when pressing Enter.
* Fixed the "Select Excel file" dropdown resetting on branch selection.
* Fixed Run and Trace features malfunctioning with context fields in property-containing tables.
* Fixed the "Failed to open project" error when opening from deployment with "Flat folder structure" disabled.
* Fixed performance degradation caused by extra dependencies in environment tables.
* Fixed compilation failing to restart on module updates with cross-project dependencies.

### Maven Plugin

* Fixed the Maven build failing with `NullPointerException`.

### Core

* Fixed a test failure caused by incorrect default value comparison.
* Fixed incorrect error messages in transposed rules table return expressions.
* Fixed the array index column being incorrectly selected for inherited datatypes.
* Fixed a common spreadsheet step cell type being incorrectly defined as object.
* Fixed an external return not being matched with a smart lookup table when the horizontal condition height is greater
  than 3.

### Demo

* Fixed module deletion being blocked after a file upload in the Repository tab.
* Fixed Groovy script projects failing deployment in the demo package.

## Library Updates

| Library              | Version               |
|:---------------------|:----------------------|
| Spring Framework     | 5.3.10                |
| Spring Boot          | 2.5.5                 |
| Apache CXF           | 3.4.5                 |
| Tomcat               | 9.0.54                |
| Jetty                | 9.4.44.v20210927      |
| Groovy               | 3.0.9                 |
| Apache Kafka         | 2.8.1                 |
| Elasticsearch        | 6.8.19                |
| Spring Elasticsearch | 3.2.13.RELEASE        |
| JGit                 | 5.13.0.202109080827-r |
| Swagger              | 1.6.3                 |
| Swagger UI           | 3.52.3                |
| Joda Time            | 2.10.12               |
| OWASP                | 6.3.2                 |

---
title: OpenL Tablets 5.24.7 Release Notes
date: 2021-10-14
description: Fixed 14 bugs across the core engine, WebStudio, repository, and demo components,
    and updated libraries including Spring, Tomcat, Jetty, Groovy, and Elasticsearch.
---

OpenL Tablets **5.24.7** is a maintenance release that resolves 14 bugs across the core engine, WebStudio, repository,
and demo components, and updates libraries.

## Bug Fixes

### Core

* Fixed the index column being incorrectly selected for an array when the datatype is inherited.
* Fixed increased memory consumption in Rule Services.
* Fixed the cell type for a common spreadsheet step being defined as an object when the same step has a defined type in
  other steps.
* Fixed the `ArrayIndexOutOfBoundsException` error being logged for smart rules with unmatched titles.
* Fixed slow performance caused by extra dependencies in environment tables.

### WebStudio

* Fixed an error appearing in the log when a user closes or deletes a project after exporting it.
* Fixed the revisions list disappearing when opening a project while the Revisions tab is displayed.
* Fixed new fields appearing in the Admin tab for the design repository when a user presses Enter.
* Fixed incorrect UI display in the Admin tab when a repository name is too long.
* Fixed the "Project 'MyProject' is not found" error being displayed when receiving an update from a branch where the
  current module is deleted.
* Fixed the "Select Excel file" drop-down list being reset when a user selects another branch.

### Repository

* Fixed the "WebStudio can't open two projects with the same name" error being displayed when opening a revision of the
  deploy configuration.

### Demo

* Fixed a project with Apache Groovy scripts not being deployable in the demo.
* Fixed a user being unable to delete a module after a file is uploaded to the project in the Repository tab.

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

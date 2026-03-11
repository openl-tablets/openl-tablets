---
title: OpenL Tablets 5.24.5 Release Notes
date: 2021-07-21
description: Improved menu button visibility in WebStudio, fixed 11 bugs across the core engine, WebStudio,
    and Rule Services, and updated libraries including Spring, Tomcat, Jetty, and Elasticsearch.
---

OpenL Tablets **5.24.5** is a maintenance release that improves menu button visibility in WebStudio, resolves 11 bugs
across the core engine, WebStudio, and Rule Services, and updates libraries.

## Improvements

### WebStudio

* Menu buttons for **Run**, **Run into File**, **Trace**, and **Trace into File** now remain visible regardless of
  spreadsheet input length, decision table complexity, or test case quantity.

## Bug Fixes

### Core

* Fixed the `LOB` runtime context property disappearing during rules execution.
* Fixed no error message being displayed when a non-existing rule is called.
* Fixed an error not being displayed when there is a duplicate `C1` column in the rules table.
* Fixed BEX syntax being case-sensitive in decision tables.
* Fixed the `java.lang.UnsupportedOperationException: Multi-reference assignment is not supported` error not being
  displayed after project compilation, only after running a test.
* Fixed a misleading hint being displayed for the output object in smart rules.

### WebStudio

* Fixed the Compare Excel files window not opening when the Table Dependencies window is displayed.
* Fixed a non-informative error being displayed when updating a module that is open in Excel.
* Fixed a loader being displayed to the user every 5 seconds on a project page in the Editor.
* Fixed a `NullPointerException` error being displayed in the log when opening a datatype table divided into table
  parts.

### Rule Services

* Fixed an ambiguous status code being received in responses for null and non-null values.

## Library Updates

| Library          | Version          |
|:-----------------|:-----------------|
| Spring Framework | 5.3.9            |
| Spring Boot      | 2.5.2            |
| Spring Security  | 5.5.1            |
| Tomcat           | 9.0.50           |
| Jetty            | 9.4.43.v20210629 |
| Elasticsearch    | 6.8.17           |
| Cassandra Driver | 4.12.0           |
| Slf4j            | 1.7.31           |
| Commons IO       | 2.11.0           |
| Swagger UI       | 3.51.1           |
| HikariCP         | 4.0.3            |
| ASM              | 9.2              |

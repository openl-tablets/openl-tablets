---
title: OpenL Tablets 5.25.5 Release Notes
date: 2021-12-17
description: Fixed 4 bugs in WebStudio and the core engine, updated Log4j, Spring, Tomcat, and other libraries, with a known issue in Swagger UI navigation.
---

OpenL Tablets **5.25.5** is a maintenance release that fixes 4 bugs in WebStudio and the core engine, and updates
libraries.

## Bug Fixes

### WebStudio

* Fixed WebStudio crashing on project opening when a step with the same name is set equal to an alias in one table and
  equal to a variable of an alias type in another table.

### Core

* Fixed a smart lookup horizontal condition being matched incorrectly with input data when columns are merged.
* Fixed a smart lookup table with an external return not being compiled when some cells are merged.
* Fixed `NullPointerException` being displayed in a decision table with an invalid parameter declaration.

## Library Updates

| Library          | Version      |
|:-----------------|:-------------|
| Log4j            | 2.16.0       |
| Spring Framework | 5.3.13       |
| Spring Boot      | 2.6.1        |
| Tomcat           | 9.0.56       |
| Netty            | 4.1.72.Final |
| Bouncy Castle    | 1.70         |
| Elasticsearch    | 6.8.21       |
| Swagger UI       | 4.1.3        |
| MS SQL JDBC      | 9.4.1.jre8   |
| Mockito          | 4.1.0        |
| Awaitility       | 4.1.1        |

## Known Issues

* Clicking the Swagger link navigates the user to an incorrect screen.
  **Workaround**: Copy the link to `openapi.json`, paste it into the URL field, and click the **Explore** button.

---
title: OpenL Tablets 5.25.9 Release Notes
date: 2022-03-31
description: Improved number formatting, added several validation warnings, fixed 12 bugs across the core engine, WebStudio, Rule Services, and Maven plugin, and updated libraries.
---

OpenL Tablets **5.25.9** is a maintenance release that improves number formatting, adds several validation warnings,
resolves 12 bugs across the core engine, WebStudio, Rule Services, and the Maven plugin, and updates libraries.

## Improvements

### Core

* Added a `toString()` method for number formatting.
* Added a warning when rule field names differ from datatype field names.
* Added a warning when comparing a vocabulary value to a non-vocabulary value.
* Added a warning for duplicate alias table names across modules.

### Demo

* On macOS, `start.sh` is now executable via double-click.

## Bug Fixes

### Core

* Fixed the `add(Date, Integer)` operation not being null-safe.
* Fixed an extra warning message "Ambiguous matching" being displayed for a smart lookup when there is no other
  candidate for column matching.
* Fixed an incorrect warning message being displayed when there is a reference to a spreadsheet cell whose name contains
  a space.
* Fixed a column being matched incorrectly with the output parameter when a preposition is used in the input parameter
  field name.
* Fixed `StringIndexOutOfBoundsException` being thrown with no hint message displayed when there is a cast in a ternary
  operator.
* Fixed default values in the datatype table not being validated against an alias when the alias type is numeric.

### WebStudio

* Fixed a user name not being displayed upon page restart after settings are applied.
* Fixed the incorrect message "File elements are identical" being displayed in the Compare tab when there are actual
  differences between versions.
* Fixed table headers being displayed in the Repository tab when no project is loaded.

### Rule Services

* Fixed build failure or deployment failure occurring when Swagger annotations are used for the `annotationTemplate`
  service.
* Fixed an incorrect project being opened in Rule Services when clicking the Swagger link.

### Maven Plugin

* Fixed the `verify` goal failing on Groovy scripts.

## Library Updates

| Library          | Version      |
|:-----------------|:-------------|
| Spring Framework | 5.3.17       |
| Spring Security  | 5.6.2        |
| Spring Boot      | 2.6.5        |
| Apache CXF       | 3.5.1        |
| Log4j            | 2.17.2       |
| Jackson          | 2.13.2       |
| Jackson Databind | 2.13.2.1     |
| Tomcat           | 9.0.60       |
| Netty            | 4.1.75.Final |
| Groovy           | 3.0.10       |
| Swagger UI       | 4.9.1        |
| Mockito          | 4.4.0        |
| Awaitility       | 4.2.0        |
| OWASP            | 7.0.1        |

---
title: OpenL Tablets 5.24.2 Release Notes
date: 2021-05-26
description: Added CORS support in WebStudio, improved condition expressions and array support in decision tables,
    fixed over 40 bugs across the core engine, WebStudio, repository, Rule Services, Docker, and Maven plugin.
---

OpenL Tablets **5.24.2** is a feature release that introduces CORS support in WebStudio, significant improvements to
condition expression capabilities and array support in decision tables, and resolves over 40 bugs across multiple
components.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## New Features

### WebStudio

* Added CORS support, enabling the REST API to be called from applications running on different hosts or ports.

## Improvements

### Core

* Condition parameters are now accessible from other condition expressions, actions, and returns in rules and smart
  rules tables.
* Added support for two-dimensional arrays in decision table conditions.
* Enabled mixing of expressions and raw values in decision tables.
* `SpreadsheetResult` values can now be used as rule input parameters and tested in test tables.
* Added support for Hive partitioning columns for statement creation and insertion.
* Added Hive connection pool support.

### WebStudio

* Git repository URLs are now automatically converted from HTTP to HTTPS without modifying the local path.
* Updated the repository name validation pattern with expanded allowed symbols and increased length limits.

## Bug Fixes

### Core

* Fixed the `contains` function malfunctioning with string ranges containing numbers without leading zeros.
* Fixed incomplete Java syntax format support as parameter types in rules tables.
* Fixed the "Variable 'b' is already defined" error not being displayed at the top of a rules table.
* Fixed a `NullPointerException` being displayed for inconsistent return expressions in a rules table.
* Fixed the "There is no index 0 in the sequence" error for inconsistent condition expressions.
* Fixed a `NullPointerException` caused by spaces in array declarations within action columns.
* Fixed a `NullPointerException` caused by lexical errors in conditions.
* Fixed a `NullPointerException` caused by unclosed parentheses in conditions.
* Fixed a `NullPointerException` caused by spaces in array declarations in conditions.
* Fixed arrays being incompatible with external conditions.
* Fixed missing `class` information in OpenL Tablets datatypes.
* Fixed external conditions being incompatible with input parameters in alternative expressions.
* Fixed return parameter types being ignored during validation in rules tables.
* Fixed case-insensitive spreadsheet step references.
* Fixed missing warnings in method tables with multiple return rows.
* Fixed `expirationDate` and `endRequestDate` properties not being time-sensitive.

### WebStudio

* Fixed spreadsheet results being incorrectly created from full paths marked in OpenAPI Scaffolding.
* Fixed the "Expected operation is not found for path" error in projects with duplicate methods.
* Fixed the SAML load-balancer/reverse-proxy checkbox being non-functional.
* Fixed the repository name length being unlimited, causing UI corruption.
* Fixed 12 AM and 12 PM being displayed identically.
* Fixed a branch disappearing when its name contains `.lock/`.
* Fixed projects with Cyrillic usernames entering view mode immediately.
* Fixed the "Project is locked by other user" error for Cyrillic usernames.
* Fixed national symbol usernames being displayed incorrectly in the Status column.
* Fixed national symbols in project names being skipped when generating branch names.
* Fixed a non-informative error being shown when saving duplicate user groups.
* Fixed the "Failed to save changes" error occurring on Trace/Run JSON when the table contains errors.
* Fixed Groovy classes not working in test tables under Java 8 and Java 15.
* Fixed an internal server error when moving the Return column near the end of a table.
* Fixed an "Out of memory" error with multiple user groups in the Admin tab.
* Fixed a corrupted UI when a project is not found.
* Fixed the "Sorry! Page Not Found!" error when accessing dependent module datatypes or methods.
* Fixed non-informative error messages being displayed for absent methods in modules.
* Fixed numbers being displayed in E notation in Trace.
* Fixed OpenAPI reconciliation failing after restoring a previous Excel version.
* Fixed the version field not being deletable in rules deploy configuration.
* Fixed the error message for used parameters not being displayed at the top of the table.
* Fixed SAML authentication failing when the server forbids application context modification.
* Fixed uninformative error messages unrelated to tables being displayed.
* Fixed missing "Passed"/"Failed" markers in compound result steps.
* Fixed an error when clicking the **Previous** button in data table creation.
* Fixed the version setting not being possible in the Rules Deploy Configuration tab.
* Fixed an internal server error when the Active Directory connection is unavailable.

### Repository

* Fixed missing design repository access rights blocking all projects.
* Fixed the "Repository 'Design': null" error caused by invalid Git URLs.
* Fixed a `NullPointerException` being logged with non-existing repository names in configuration.
* Fixed deploy configuration repository properties being lost after migration from version 5.23.x to 5.24.0.

### Rule Services

* Fixed rules not being redeployed after Git merge requests.
* Fixed startup failure with a `repo-zip` repository when required properties are missing.

### Docker

* Fixed WebStudio, Rule Services, and demo images failing when running as non-root users.

### Maven Plugin

* Fixed an infinite loop occurring when executing two `package` goals.

## Library Updates

| Library             | Version      |
|:--------------------|:-------------|
| Hibernate           | 5.4.24.Final |
| Hibernate Validator | 5.4.3.Final  |

---
title: OpenL Tablets 5.25.7 Release Notes
date: 2022-01-27
description: Improved WebStudio progress bar and dependency management, fixed 21 bugs across the core engine and WebStudio, and updated libraries.
---

OpenL Tablets **5.25.7** is a maintenance release that improves the WebStudio progress bar and dependency management,
resolves 21 bugs across the core engine and WebStudio, and updates libraries.

## Improvements

### WebStudio

* The "Compilation" text has been replaced with "Loading" on the progress bar.
* In the Manage Dependencies window, the **All modules** checkbox is no longer selected by default.

## Bug Fixes

### Core

* Fixed the "string index out of range" error being displayed when a 2-dimensional array is defined in a data table.
* Fixed the `NullPointerException` error being displayed in a datatype table containing table parts.
* Fixed the `StringIndexOutOfBoundsException` error being shown in the log when opening a decision table consisting of
  table parts.
* Fixed the `NullPointerException` error being displayed and actual compilation problems being hidden for a smart rules
  table containing table parts.
* Fixed the `ArrayIndexOutOfBoundsException` error being displayed in data tables when a table definition contains
  invalid symbols.
* Fixed the `Collect` operator not working correctly for lists in a smart lookup table.
* Fixed a transposed rules table with horizontal conditions not being compiled.
* Fixed the column being incorrectly matched to the input object field instead of the output object when there is a 100%
  match with an output object field name in smart rules.
* Fixed a datatype link not being shown when the datatype attribute is marked as internal with `~`.

### WebStudio

* Fixed datatypes not having hints and not being underlined in cast operations.
* Fixed a user being redirected to the Editor page instead of the current project page when receiving an update from a
  branch where the current module is deleted.
* Fixed the **Projects to Deploy** list not being updated for deploy configurations when modified via another WebStudio
  instance.
* Fixed the "The element is null" error being displayed when there are two tables with the same name.
* Fixed a Git commit unrelated to the project being displayed in the revision history and causing export of an incorrect
  version.
* Fixed the `NullPointerException` error being displayed when the `SpreadsheetResult` constructor is called.
* Fixed the `StringIndexOutOfBoundsException` being shown in a log file when the cast-to-array operation is used in a
  step name.
* Fixed no hint with a datatype being displayed for a spreadsheet when the dependent project contains spreadsheets with
  the same step name but different capitalization.
* Fixed the `StackOverflowError` not being displayed in the trace pop-up window.
* Fixed database settings entered in the Installation Wizard for storage users not being saved.
* Fixed an unfriendly authentication error message when the repository is stored on a remote GitHub server.

## Library Updates

| Library          | Version      |
|:-----------------|:-------------|
| Spring Framework | 5.3.15       |
| Spring Security  | 5.6.1        |
| Spring Boot      | 2.6.2        |
| Apache CXF       | 3.5.0        |
| Log4j            | 2.17.1       |
| Jackson          | 2.13.1       |
| Netty            | 4.1.73.Final |
| Hibernate        | 5.4.33.Final |
| Swagger          | 1.6.4        |
| Mockito          | 4.2.0        |
| OkHttp           | 4.9.3        |
| Apache Ant       | 1.10.12      |
| OWASP            | 6.5.3        |

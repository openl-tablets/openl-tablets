---
title: OpenL Tablets 5.24.6 Release Notes
date: 2021-08-30
description: Fixed 26 bugs across the core engine, WebStudio, repository, and Rule Services components,
    and updated libraries including Spring Boot, Tomcat, Elasticsearch, and Netty.
---

OpenL Tablets **5.24.6** is a maintenance release that resolves 26 bugs across the core engine, WebStudio, repository,
and Rule Services components, and updates libraries.

## Contents

* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Bug Fixes

### Core

* Fixed the "Failure in the method: autocast" error being displayed when a method is called for an array.
* Fixed the `flatten(Datatype[], Datatype[])` function returning `Object[]` instead of `MyDatatype[]`.
* Fixed a `NullPointerException` error being displayed in a data table.
* Fixed a `ClassCastException` error being displayed when running a table with condition expressions that access a
  return value.
* Fixed an extra dimension being added to the condition input parameter of an array type.
* Fixed merged cells in conditions being interpreted as `C1` instead of `MC1` in smart rules and smart lookup tables.
* Fixed a `NullPointerException` error being displayed when an external condition contains an error.
* Fixed a `NullPointerException` error being displayed when columns cannot be matched properly in a simple lookup table.

### WebStudio

* Fixed a `StringIndexOutOfBoundsException` error appearing in the log when a ternary operation is used in a spreadsheet
  table.
* Fixed empty aliases being incorrectly displayed in the Rules Editor.
* Fixed the "Project source file was modified" pop-up being displayed on editing and branch merge in the Rules Editor.
* Fixed a user being redirected to the module page after editing a table that overflows another table in an Excel file.
* Fixed a module not opening when the last symbol in the Excel filename is a space.
* Fixed a `NullPointerException` error appearing when a spreadsheet table step contains a ternary operation and is cast
  to an array.
* Fixed long commit comments being split into two lines in commit history.
* Fixed a `NullPointerException` error appearing when a condition title is empty.
* Fixed the Test button in the table menu running tests only in the currently opened module.
* Fixed the horizontal scroller appearing with many user group privileges.
* Fixed the Test button not being displayed when tests are stored in another module.
* Fixed the wrong total error count being displayed in the table tree with two active table versions.
* Fixed the `java.io.IOException: Unable to delete directory` error being displayed when saving a project with
  dependents.

### Repository

* Fixed a `NullPointerException` error being displayed when a project folder is deleted outside WebStudio from a
  multi-project branch.
* Fixed the "Cannot delete the branch" error being displayed when attempting to delete a branch with an open project.
* Fixed a local project appearing after branch deletion.

### Rule Services

* Fixed a request failing with a `NullPointerException` when using a short condition alias in a rules table return.
* Fixed incorrect step names being displayed in the JSON response for spreadsheet steps when
  `jackson.propertyNamingStrategy` is set.

## Library Updates

| Library          | Version      |
|:-----------------|:-------------|
| Spring Boot      | 2.5.4        |
| Spring Security  | 5.5.2        |
| Tomcat           | 9.0.52       |
| Netty            | 4.1.67.Final |
| Elasticsearch    | 6.8.18       |
| Slf4j            | 1.7.32       |
| Cassandra Driver | 4.13.0       |
| Zookeeper        | 3.7.0        |
| Commons Compress | 1.21         |
| Swagger UI       | 3.51.2       |
| XStream          | 1.4.18       |
| Apache Ant       | 1.10.11      |
| MS SQL JDBC      | 9.4.0.jre8   |

---
title: OpenL Tablets 5.26.2 Release Notes
date: 2022-10-06
description: HTTP header propagation into rules execution, SpreadsheetResult beans as input parameters,
    external tools attachment, extensive bug fixes, and library updates.
---

OpenL Tablets **5.26.2** is a maintenance release introducing HTTP header propagation, SpreadsheetResult beans as input parameters, external tools attachment in WebStudio, extensive bug fixes, and library updates.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## New Features

### HTTP Header Propagation

The capability to propagate an HTTP header into the OpenL Tablets rules execution is now available.

### SpreadsheetResult Beans Support

SpreadsheetResult beans are now supported as input parameters in OpenL Tablets Rule Services.

## Improvements

### Core

* Spaces and special symbols `+`, `%`, `()`, `&` are now supported for the localization function `msg()`.
* The `$ref` properties are replaced with `_ref_` properties for environment variables.

### Rule Services

* Environment in Dockerfile can now be customized before starting Jetty.
* Exception messages are more informative in case of Rule Services failure.
* The list of available services is ordered alphabetically.

### WebStudio

* An ability to attach external tools is introduced.

## Bug Fixes

### Core

* Fixed an error identifying that the spreadsheet step cannot be found when a decision table has a custom spreadsheet result input parameter.
* Fixed boolean, date, and integer literals not working in a spreadsheet.
* Fixed the "Cannot parse field name." error displayed for the expression in the rules table condition.
* Fixed numerous errors displayed if the vocabulary has the array datatype.
* Fixed `[Ljava.lang.String;@c33e738` and `[Ljava.lang.String;@6c55f2f9` errors displayed in the drop-down lists of tables.
* Fixed empty array elements being trimmed.
* Fixed only the first rule working properly if BEX grammar is used in the DT condition.

### WebStudio

* Fixed a project not being deployable to a Git repository.
* Fixed "Sorry! Something went wrong." error displayed on opening a module that contains a medium-size rule with errors.
* Fixed "Cannot load the module: NullPointerException:" error displayed in the error section for the transform operation.
* Fixed `org.openl.rules.helpers.RulesUtils.copy(java.lang.Object)` method failing on running the tests from another module.
* Fixed "X" having inconsistent behavior in the Actions column in the Repository tab if a non-main branch is selected.
* Fixed `NullPointerException` displayed in logs on project closure for a previously deleted project when using two browsers.
* Fixed methods being highlighted incorrectly if there is a line break in the expression.
* Fixed a typo in the OpenAPI file configuration.
* Fixed "Project not found" 404 error displayed if a user opens or closes a project in the Repository tab.
* Fixed method name being incorrectly highlighted if the method is called in braces.
* Fixed user profile settings being locked for editing in OpenL Tablets WebStudio in a single-user mode.
* Fixed "Sorry! Something went wrong." error displayed on running a test table that has a reference to an erroneous table.
* Fixed `NullPointerException` displayed in trace for column match tables.
* Fixed incorrect text being underlined in the expanded error message.
* Fixed horizontal scroll overlapping the open icon for a node in a trace table.

### Demo

* Fixed projects not being deployable or re-deployable after a specific project is deployed.

### Rule Services

* Fixed OpenL Tablets Rule Services throwing `ClassCastException` on calling a method generated for a data table of the SpreadsheetResult or CustomSpreadsheetResult type.
* Fixed `java.util.HashMap` class displayed for cells of AnySpreadsheetResult in OpenL Tablets Rule Services.
* Fixed `propertyNamingStrategy` not applied to the AnySpreadsheetResult fields.
* Fixed a path to the non-existing properties file being logged.

### Repository

* Fixed if entered settings cannot be applied, default settings are restored.

### Maven Plugin

* Fixed the `verify` Maven goal not performing a check on whether the target project can be successfully deployed.

## Library Updates

| Library                | Version            |
|:-----------------------|:-------------------|
| Spring Framework       | 5.3.23             |
| Spring Security        | 5.7.3              |
| Spring Boot            | 2.7.3              |
| Jackson                | 2.13.4        м    |
| Kafka Clients          | 3.2.3              |
| Netty                  | 4.1.82.Final       |
| Jetty                  | 10.0.12            |
| Swagger UI             | 4.14.0             |
| Avro                   | 1.11.1             |
| Amazon AWS SDK         | 1.12.305           |
| Azure Blob Storage SDK | 12.19.1            |
| JGit                   | 6.3.0.202209071007 |
| MSSQL Driver           | 11.2.1.jre11       |
| Hibernate              | 5.6.11.Final       |
| Hibernate Validator    | 6.2.5.Final        |
| GreenMail              | 1.6.10             |
| Gson                   | 2.9.1              |
| Groovy                 | 3.0.13             |
| BouncyCastle           | 1.71.1             |
| Joda Time              | 2.11.1             |
| XMLSecurity            | 2.3.2              |
| SnakeYAML              | 1.32               |
| Cloning                | 1.10.3             |

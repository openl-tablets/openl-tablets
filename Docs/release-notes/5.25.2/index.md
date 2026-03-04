---
title: OpenL Tablets 5.25.2 Release Notes
date: 2021-11-17
description: Reduced StringRange memory usage, improved Rule Services database reconnection, fixed 14 bugs across the core engine and WebStudio, and updated libraries.
---

OpenL Tablets **5.25.2** is a maintenance release that reduces memory usage for `StringRange`, improves database
reconnection behavior in Rule Services, resolves 14 bugs across the core engine and WebStudio, and updates libraries.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

### Core

* Reduced memory usage when `StringRange` is used.
* Added a warning message when comparing values of different types.

### Rule Services

* If the database is unavailable on application startup, Rule Services now repeats connection attempts until a
  connection is established.

## Bug Fixes

### Core

* Fixed a datatype field of the `SpreadsheetResult` type not being settable dynamically.
* Fixed a custom `SpreadsheetResult` field not being definable when the spreadsheet is stored in the same module.
* Fixed external conditions not being matched to a smart lookup table.
* Fixed rule compilation failing with the "Invalid Decision Table header" error when the first column is commented.
* Fixed validation for the alias data type not working when several alias parameters are passed with a non-alias
  parameter between them.
* Fixed the time zone being ignored while parsing the date when the cell has a text type.
* Fixed no error message appearing when the `contains` function used in a condition is case-sensitive but an incorrect
  parameter is specified.
* Fixed a method not being found by the full path when an array is passed as input.
* Fixed the "Identifier not found" error being displayed when comparing an element to an array using simplified syntax.

### WebStudio

* Fixed the "Something went wrong" error appearing when reverting changes while an Excel file is open.
* Fixed tags not being set and a "Something went wrong" error appearing when a user creates a project by importing from
  a Git repository, sets tags, and clicks the Import button a second time.
* Fixed an obsolete number of errors being displayed in the collapsed error panel in Firefox.
* Fixed the "URL" text not being in capital letters in the "CAS server url" and "SAML server metadata url" field names.
* Fixed incorrect field labels being displayed in the Copy Deployment Configuration tab.

## Library Updates

| Library          | Version      |
|:-----------------|:-------------|
| Spring Framework | 5.3.13       |
| Spring Security  | 5.6.0        |
| Spring Boot      | 2.5.6        |
| Netty            | 4.1.70.Final |
| Elasticsearch    | 6.8.20       |
| Swagger UI       | 4.1.0        |
| Joda Time        | 2.10.13      |
| OWASP            | 6.5.0        |

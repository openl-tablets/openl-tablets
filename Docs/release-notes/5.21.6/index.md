---
title: OpenL Tablets 5.21.6 Release Notes
date: 2018-09-14
description: Introduces new Date functions (toDate, toString, Date), copy object functionality, JBoss EAP 7.1 support,
    and 15 bug fixes for WebStudio, Core, and Maven plugin.
---

OpenL Tablets **5.21.6** introduces new Date functions and includes improvements and bug fixes.

## New Features

### New Date Functions

Three new Date functions are now available:

* `toDate()` — converts a string to a `Date`. Example: `toDate("7/12/80")` → `12 July 1980 (00:00:00.000)`
* `toString()` — converts a `Date` to a string. Example: `toString(Date)` → `"07/12/1980"`
* `Date()` — creates a `Date` from year, month, and day. Example: `Date(2018, 7, 12)` → `12 July 2018 (00:00:00.000)`

## Improvements

**Rule Service:**

* Error codes changed for REST services.

**Core:**

* Added `copy` object functionality.

**Rule Service & WebStudio:**

* Support for JBoss EAP 7.1.

## Bug Fixes

**WebStudio:**

* Fixed: DB driver is not registered if the user did not select a DB connection on Step 3 of the Installation Wizard.
* Fixed: Failed to open a module that has an error without a message.
* Fixed: OpenL fails to save changes in a table if the initial Excel file was edited on macOS.
* Fixed: Internal Server Error appears in the Trace window.
* Fixed: Values from alias datatype are displayed in the drop-down when selecting a `String` datatype in Run/Trace
  functionality.
* Fixed: Project export hangs if the Excel file is open.
* Fixed: Button "Insert row before" actually inserts a row after — the button has been renamed to "Insert row after".

**Core:**

* Fixed: Null Pointer Exception in unary `+` and `-` operators.
* Fixed: Error "Duplicate type definition" if one project depends on another project, both have datatypes with the same
  name but in different datatype packages.
* Fixed: Memory consumption in `SpreadsheetResult`.
* Fixed: Null-safety does not work with `DoubleValue` for the `avg()` function.
* Fixed: Incorrect calculation in Spreadsheet if the `parallel` property is set to `FALSE`.

**Maven Plugin:**

* Fixed: Test compilation fails in the OpenL Maven Plugin when a Data table is located in a dependency module.
* Fixed: OpenL tests fail with `OutOfMemoryError` in a large test suite.
* Fixed: `openl-maven-plugin` does not package dependencies if the `runtime` scope is defined in transitive
  dependencies.

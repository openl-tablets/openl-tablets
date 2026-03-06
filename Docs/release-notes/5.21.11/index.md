---
title: OpenL Tablets 5.21.11 Release Notes
date: 2018-12-14
description: Performance improvements to Spreadsheet copy and threading, SmartRules hints, and 13 bug fixes for WebStudio navigation, Trace functionality, and Core rule evaluation.
---

OpenL Tablets **5.21.11** is a patch release with improvements and bug fixes.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## Improvements

**Core:**

* Improved performance of the `copy()` method.
* Removed thread locks on Spreadsheet tables.

**WebStudio:**

* SmartRules: Added hints to the return columns.

## Bug Fixes

**WebStudio:**

* Fixed: "Something went wrong" error presented when the user selects "by File" in the table tree.
* Fixed: "Something went wrong" error when navigating via the "Back" button or a Table Link.
* Fixed: Incorrect text in the Installation wizard's "Configure initial users" section.
* Fixed: "Something went wrong" error on table opening with 2 modules containing datatypes with the same name.
* Fixed: Trace functionality hangs on tests for large projects.
* Fixed: Warnings appear in the log file for Trace functionality.
* Fixed: Incorrect link to resources (missing `contextPath`).
* Fixed: SmartRules: Incorrect hint for return column — Alias Datatype in a complex object.
* Fixed: SmartRules: Incorrect hints if condition is matched on a custom datatype field.

**Core:**

* Fixed: Method search does not work with Generics and Arrays.
* Fixed: Null-safety not working in index operations.
* Fixed: SmartRules: No error on compilation if Conditions or Return Values do not match alias datatype.
* Fixed: SmartRules: Error on UI if return type is a custom type with multiple fields and only 1 return column.

> **Note:** Old ModeShape-based repositories are no longer supported, and all related libraries have been removed.

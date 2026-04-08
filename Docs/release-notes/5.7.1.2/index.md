---
title: OpenL Tablets 5.7.1.2 Release Notes
date: 2010-08-18
description: Maintenance release fixing 6 bugs in Decision Tables, date arithmetic, TBasic tests, and WebStudio,
    and adding datatype inheritance, expected error specification in test methods, and improved trace view.
---

OpenL Tablets **5.7.1.2** is a maintenance release with bug fixes and improvements.

## Improvements

* Datatype can inherit from another type.
* Expected error messages can be specified in the TestMethod component.
* Added "Test" button for a single test case in TestMethod.
* Updated trace view to show errors.
* Updated Datatype table wizard with the ability to specify a parent type.
* Removed code and parameter declarations for simple cases of the RET column.

## Bug Fixes

* Fixed: Decision Table execution crashes when a cell in the Return column has no value.
* Fixed: Date subtraction returns the wrong number of days between dates.
* Fixed: Cannot create a new project from an Excel template when WebStudio is deployed as a WAR.
* Fixed: Exceptions during creation and execution of test tables for TBasic tables.
* Fixed: WebStudio does not recognize `"yes"`/`"no"` values as `true`/`false`.
* Fixed: No icons and incorrect text description in Trace for the ColumnMatch `<WEIGHTED>` component.

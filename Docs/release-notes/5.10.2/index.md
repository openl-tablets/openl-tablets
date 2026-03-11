---
title: OpenL Tablets 5.10.2 Release Notes
date: 2013-06-06
description: Adds implicit Double comparison in tests, full SpreadsheetResult display for TBasic tests, Web Services
    independence from OpenL Core, and fixes 5 bugs in memory leaks, SpreadsheetResult binding, and WebStudio display issues.
---

OpenL Tablets **5.10.2** is a patch release with improvements and bug fixes.

## Improvements

**Core:**

* Introduced implicit comparison of expected and returned results for `Double` types, accounting for meaningful digits
  when running tests.

**WebStudio:**

* Enhanced display of complete `SpreadsheetResult` output for TBasic and other table type rules during test execution.

**Web Services:**

* Achieved independence from OpenL Core libraries by copying runtime context classes into a new package.

## Bug Fixes

**Core:**

* Fixed: Memory leak issues.
* Fixed: Error when binding custom `SpreadsheetResult` types caused by the sequence of tables in a file sheet.

**WebStudio:**

* Fixed: Incorrect display of `True`/`False` values for every tested cell of `SpreadsheetResult`.
* Fixed: Incorrect HTML element on the test result screen.
* Fixed: `javax.servlet.ServletException` (Error 500) on WebSphere AS when saving tables with opened Excel files.

---
title: OpenL Tablets 5.7.5 Release Notes
date: 2011-04-04
description: Adds edit history revert in WebStudio, Excel-like attribute access syntax, array method application,
    thread-safe instances, expanded range support, and fixes 12 bugs in test tables, TableEditor, WebStudio, and the rules repository.
---

OpenL Tablets **5.7.5** is a patch release with new features, improvements, and bug fixes.

## New Features

### Revert Changes in WebStudio

A new dialog shows the edit history for a module between check-ins to the repository, allowing selective revert of
individual changes.

### Attributes Access Methods

New Excel-like syntax for accessing class attributes is now supported.

### Rules and Methods Applied to Arrays

All single-parameter methods can now be applied to an array of elements, enabling array-level operations.

## Improvements

**Core:**

* Thread-safe dynamic wrapper instances.
* Thread-safe `EngineFactory` instances.
* `IntRange` now supports all integral types; `DoubleRange` supports all numeric types.
* Added support for negative numbers in `DoubleRange`.
* Added `%` (modulo) operator.
* Operators `++x`, `x++`, `--x`, `x--`, `~x`, `|x|`, `+x` work consistently across all numeric types.

**TableEditor:**

* Improved cell value formatting.
* Ability to switch from any editor to the text editor.

**Rules Repository:**

* Performance improvements.

**Browser Compatibility:**

* All TableEditor features work in Chrome and Firefox 4.

## Bug Fixes

* Fixed: Incorrect test status for `null` expected result in test tables.
* Fixed: Float value displayed and parsed incorrectly in the test table.
* Fixed: Cannot define Boolean `OR`/`AND` operators in table body as `or` and `and`.
* Fixed: Colors from the first two columns of the theme palette are not applied in TableEditor.
* Fixed: Comments do not move after properties edition in TableEditor.
* Fixed: Exception after saving a table with an empty formula cell in TableEditor.
* Fixed: Table loses formulas during copying in WebStudio.
* Fixed: Properties added incorrectly for a datatype table placed in an `.xlsx` file in WebStudio.
* Fixed: Cannot use "Edit as new version" and "Copy" for some Spreadsheet tables in WebStudio.
* Fixed: Cannot select a project with a name containing the `&` symbol in the rules repository.
* Fixed: Wrong sequence of repository update events causes incorrect service updates in Web Services.
* Fixed: Trace for Spreadsheet table causes exception on Linux and macOS.

## Known Issues

* **`.xlsx` colors**: Do not use colors from the first two columns of the color theme palette in Excel, as they are not
  supported correctly.
* **`.xls` colors**: If legacy `.xls` files contain Excel 2007 theme colors, they will be replaced with indexed colors
  on the first save in WebStudio.
* **Java for Datatypes**: Due to classpath priority, delete previously generated `.java` and compiled `.class` files
  before running Datatype Java generation to avoid class conflicts.

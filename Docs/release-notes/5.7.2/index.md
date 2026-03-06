---
title: OpenL Tablets 5.7.2 Release Notes
date: 2010-09-23
description: Introduces alias datatypes, datatype inheritance, and floating-point comparison with precision
    using ULP-based logic. Adds trace output to file, DoubleValue in web services,
    and fixes 28 bugs in WebStudio, the core engine, and the rules repository.
---

OpenL Tablets **5.7.2** is a feature release with new datatype capabilities, comparison improvements, and significant
bug fixes.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## New Features

### Alias Datatypes

A new datatype based on a standard type such as `int` or `String` with restrictions on allowable values. Inside OpenL
rules the alias type is used under its own name, but exposed as a standard type in web services or Java. Values are
validated at both compile time and execution time.

### Inheritance in Datatypes

Datatypes can now inherit from other datatypes. Cycles and inheritance from `final` types are prevented. This feature
was stabilized in this release after initial introduction in 5.7.1.2.

### Floating-Point Comparison with Precision

Standard comparison operators are changed to use ULP-based inaccuracy comparison logic for all floating-point types. New
operators are added for strict floating-point comparisons without the precision tolerance.

## Improvements

* Allow specifying a package for a Datatype in table properties.
* Allow reordering HC and RET columns in Lookup Decision Tables.
* Trace for the Dispatch by Properties component.
* Trace output to file.
* Upload project from a zip file containing a root folder.
* Links to tests from the All Tests result page.
* Integrated HTML help into WebStudio.
* Added scrolling to the repository view.
* `DoubleValue` is supported in web services.
* Colors of edit fields in the Properties section are more distinguishable.
* Default RAM increased to 1024 MB (from 512 MB) in the configuration file.

## Bug Fixes

* Fixed: WebStudio crashes after removing an `Environment` table.
* Fixed: Tree crashes when selecting a module before the previously selected one finishes loading.
* Fixed: Datatype arrays are indexed incorrectly.
* Fixed: Cannot open a Word file (`.doc`) from the Simple Search results page.
* Fixed: Properties table addition causes other tables to disappear from business view.
* Fixed: `NullPointerException` after opening a project without a `bin` folder.
* Fixed: Table cannot be edited in Excel after removing a sheet.
* Fixed: Project crashes with an incorrect version format for multi-version tables.
* Fixed: `_description_` column not displayed when viewing test results.
* Fixed: Missing informative error message above a test table when the target table contains errors.
* Fixed: Non-informative error message when project upload fails.
* Fixed: Cannot save an edited table after returning from the Rules repository view.
* Fixed: Null results for tests and run methods are not shown.
* Fixed: Exception after opening a test table with errors.
* Fixed: OpenL does not ignore empty merged rows.
* Fixed: Numbers with multiple decimal places rounded incorrectly in test results.
* Fixed: Numbers rounded in table view when Numeric format is selected in Excel.
* Fixed: Excel file colors applied incorrectly in OpenL.
* Fixed: Datatype table with a duplicate name does not appear in the project tree.
* Fixed: Missing scripts to open Excel and Word in the WebStudio WAR package.
* Fixed: Cannot create a new project in the WebStudio WAR package.
* Fixed: Exception when pressing "Run All Tests" with errors in a test table.
* Fixed: Wrong Descriptor version displayed when opening an old Deployment project version.
* Fixed: New project version opens incorrectly.
* Fixed: Unchecked-out projects can be opened for editing in Excel from the trace view.
* Fixed: Property errors not shown in the Problems section.
* Fixed: Cannot open a table in Excel if it is located outside the main spreadsheet.

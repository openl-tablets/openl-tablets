---
title: OpenL Tablets 5.7.1 Release Notes
date: 2010-08-03
description: Introduces OpenL rules project descriptors, single-Excel-file projects, a Maven archetype,
    a project resolver, and runtime context for web services. Adds multi-parameter return columns,
    column cross-references, and fixes 25 bugs in the core engine and WebStudio.
---

OpenL Tablets **5.7.1** is a major feature release with new project structure capabilities and web services
enhancements.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## New Features

### OpenL Rules Project Descriptor

An XML configuration file can be defined in a project to specify the instantiation type, the location of the Excel rules
file, and the Java class or interface to use.

### OpenL Project from Single Excel File

OpenL now loads a single Excel file directly as a rules module without requiring a project descriptor. Folders
containing only Excel files are treated as rules projects automatically.

### Maven-Based OpenL Project

An OpenL rules project Maven archetype is provided as a starting template for new rules projects.

### OpenL Rules Projects Resolver

The project descriptor is fully supported in the OpenL infrastructure. Projects following the Eclipse template
convention or containing only Excel files are also supported without a descriptor.

### Runtime Context for Web Services

The OpenL Service Frontend supports runtime-context-enabled services. An enhancer can be enabled in the service
configuration to add the runtime context as the first parameter to all rules service methods.

## Improvements

* Multiple parameters supported in the Return column of Lookup Decision Tables.
* Parameters from conditions, actions, and Return columns are accessible from other columns using
  `$<columnname>.<parameter>` syntax (for example, `$C1.min`).
* Added support for ranges in bracket notation such as `( ; ]` for `IntRange` and `DoubleRange` types.
* Java classes generated from Datatypes now include `toString`, `equals`, `hashCode`, and an all-fields constructor.
* Autocast improved for conversions from primitive types to boxed object types.
* Trace for the Spreadsheet component.
* Trace for the Method component.
* Trailing empty cells in arrays for Decision and Data tables are no longer loaded as nulls.
* Reloading of rules projects improved.
* Added help documentation to WebStudio.
* WebStudio monitors changes to Excel files on the file system.
* Auto-generated dispatch tables removed from business view.
* Wizards improved for business name, category, and technical name fields.
* Added `Date`, `DoubleRange`, `IntRange`, and `BigDecimal` types to the Datatype component wizard.
* Added validation for technical name in wizards.
* Added "Run all tests" button in the test table component view.
* Added Trace button for each test case in the test table.
* Removed the Tomahawk framework dependency from WebStudio.

## Bug Fixes

* Fixed: Return value not converted to the declared return type in a Decision Table.
* Fixed: Action not performed when an Action column is placed after the Return column.
* Fixed: `JavaWrapperGenerator` crashes with an incorrect method header.
* Fixed: `JavaWrapperGenerator` lacks support for overloaded methods with different parameters.
* Fixed: Wrong error raised for duplicate Decision Table condition headers.
* Fixed: `NullPointerException` when a datatype object is `null` in `getField`.
* Fixed: WebStudio displays errors on the current tab after login.
* Fixed: Edit in Excel not working.
* Fixed: Excel file rewritten incorrectly in WebStudio.
* Fixed: Full reload not functioning properly in WebStudio.
* Fixed: WebStudio crashes when an enum table property is set to an invalid value.
* Fixed: Error section not refreshed when switching to an error-free project.
* Fixed: Save operation hangs during Effective/Expiration date validation failure.
* Fixed: Benchmark button for a single test case is non-functional.
* Fixed: Checking/checkout button refreshes links incorrectly.
* Fixed: Multi-select not displayed in table for enum array cells.
* Fixed: Missing error message in the Problems section for duplicate table properties.
* Fixed: Problems display in the table editor functions irregularly.
* Fixed: Invalid return for tables with an unmerged properties section.
* Fixed: Wrong error for arrays defined as `1,2 3,4`.
* Fixed: Number `0.00025` displayed as `2.5E-4` in WebStudio.
* Fixed: Previously saved project always uploaded instead of the new project.
* Fixed: Copy Project functionality broken.
* Fixed: "Edit in Excel" link in TableEditor menu works incorrectly.
* Fixed: "Edit in Excel" available for read-only tables in the context menu.
* Fixed: Trace highlighting incorrect.

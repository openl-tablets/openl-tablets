---
title: OpenL Tablets 5.7.4 Release Notes
date: 2011-01-27
description: Introduces module dependency management, a rewritten rules repository with JCR 2.0 and WebDAV,
    a rule interdependency graph, explainable value types for all primitives, and fixes 23 bugs across the core, TableEditor, and WebStudio.
---

OpenL Tablets **5.7.4** is a major feature release with significant new capabilities and broad improvements.

## New Features

### Dependency Management Between Rule Modules

A new `dependency` keyword in the Environment table enables flexible separation of rules into separate modules and their
reuse. Core and custom dependency managers support configurable module loading strategies.

### Rewritten Rules Repository

The repository implementation has been completely rewritten to support additional backends. The JCR implementation is
upgraded to version 2.0 with Jackrabbit 2.1.2, and WebDAV support is included along with performance enhancements.

### Graph of Rule Interdependencies (Beta)

WebStudio now displays a graph of dependencies between rules and tests, with directional arrows and clickable rule names
for navigation.

### New OpenL-Specific Value Types

Explainable value types are added for all primitive types: `ObjectValue`, `ByteValue`, `ShortValue`, `IntValue`,
`LongValue`, `FloatValue`, `DoubleValue`, `DoubleValuePercent`, `BigIntegerValue`, `BigDecimalValue`, and `StringValue`.

## Improvements

**TableEditor:**

* Support for cell font color, boldness, italics, and underlining.
* Support for cell background color editing.
* Display of cell comments.

**Core:**

* Datatype default values are now definable in the third column.
* Classes from the classpath are used for datatypes when available.
* Multi-module feature reimplemented using the new dependency mechanism.

**WebStudio:**

* Compare Excels feature has an improved UI and algorithm.
* Three new configuration properties: Transaction Type, Custom1, Custom2.
* New projects default to checked-out status.

## Bug Fixes

* Fixed: Rules table condition code generation with `boolean`/`Boolean` parameters.
* Fixed: Alias datatypes as array components.
* Fixed: Array types disallowed as Spreadsheet cell types.
* Fixed: Property cell unmerging after saves with merged headers.
* Fixed: Cell format display with merged rows.
* Fixed: Incorrect `if` expression results in conditions.
* Fixed: Excel files with `+` or `&` characters in names.
* Fixed: Excel file editing with spaces in project names.
* Fixed: Environment table misspelling causes exceptions.
* Fixed: Duplicate empty error messages.
* Fixed: Overloaded table trace window display.
* Fixed: "Log in as local" button functionality.
* Fixed: Index Search exceptions with corrupted modules.
* Fixed: Last row formula loss when adding properties.
* Fixed: Row addition style shifting.
* Fixed: SimpleRule table editor appearance.
* Fixed: Excel format application to SimpleRule tables.
* Fixed: Array value comma recognition in WebStudio.
* Fixed: Script errors in trace for rule tables with formulas.
* Fixed: Project uploading exceptions.
* Fixed: Double-row addition in Demo.
* Fixed: Production repository projects missing properties.
* Fixed: `OutOfMemoryError` after extended table operations.

## Known Issues

* **`.xlsx` colors**: Do not use colors from the first two columns of the color theme palette in Excel.
* **`.xls` colors**: If legacy `.xls` files contain Excel 2007 theme colors, they will be replaced with indexed colors
  on the first save in WebStudio.
* **Java for Datatypes**: Delete previously generated `.java` and compiled `.class` files before running Datatype Java
  generation to avoid class conflicts.

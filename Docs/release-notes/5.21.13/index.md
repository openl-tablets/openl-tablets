---
title: OpenL Tablets 5.21.13 Release Notes
date: 2019-03-06
description: Improvements to Jackson configuration, CharRange syntax, and table properties display, plus 21 bug fixes across Core, WebStudio, Maven plugin, and web services.
---

OpenL Tablets **5.21.13** is a patch release with improvements and bug fixes.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

**Web Services & WebStudio:**

* Updated Jackson libraries to 2.9.8 to address a potential security concern.

**Web Services:**

* Enabled independent declaration of `@Path`, `@POST`, `@GET` annotations.
* Added a configuration option to disable Default Typing via `jacksondatabinding.defaultTypingMode`.
* Added configuration for the default date format via `jacksondatabinding.defaultDateFormat`.

**WebStudio:**

* Default table properties are now hidden in the table details view.

**Core:**

* `CharRange` datatype now supports the same syntax as `IntRange`.

## Bug Fixes

**Core:**

* Fixed: SmartLookup validation issue with merged rows in a Horizontal condition.
* Fixed: Test failures with a custom interface as a datatype.
* Fixed: Uninformative error message for SmartRules with custom `SpreadsheetResult`.
* Fixed: Uninformative UI message for Test table.
* Fixed: Compare feature corrupting datatypes.
* Fixed: Uninformative error message for similarly-named datatype fields with capitalization differences.
* Fixed: `CalculationStep.getStepName()` now returns values without type definition — changed from format like `MyStep:Double` to `MyStep`.

**WebStudio:**

* Fixed: `BigDecimal` Alias drop-down selection problems.
* Fixed: Missing steps in Trace.
* Fixed: Navigation failure via the blue arrow to the Properties table.
* Fixed: Compilation errors in dependent projects with equal Decision Tables.
* Fixed: Project locking issues with the Upload feature.
* Fixed: Internet Explorer 11.0 incompatibility.
* Fixed: Table Dependency feature failure with errors.
* Fixed: Backslash character disappearing after save.
* Fixed: Date selection issues from drop-down for Alias Datatype.
* Fixed: Module corruption from empty row insertion.
* Fixed: `DoubleValue` Alias validation issues.

**Maven Plugin:**

* Fixed: Verify goal failure in a clean environment with Maven 3.6.0.

**Web Services:**

* Fixed: SOAP schema containing ignored `SpreadsheetResult` properties.
* Fixed: Memory consumption increase from `custom.spreadsheet.type` property.

## Library Updates

| Library | Version |
|:--------|:--------|
| Jackson | 2.9.8   |

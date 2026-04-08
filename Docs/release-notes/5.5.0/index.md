---
title: OpenL Tablets 5.5.0 Release Notes
date: 2010-04-26
description: Introduces warnings alongside errors, gap and overlap validation for dimensional properties,
    improved Decision Table validation with enum and domain discovery,
    and fixes 8 bugs in the table editor, WebStudio, and memory management.
---

OpenL Tablets **5.5.0** is a feature release with new validation capabilities and improvements.

## New Features

### Errors and Warnings

Warnings are introduced in OpenL Tablets. Errors and warnings are now separated into fatal and non-fatal categories,
providing more precise feedback during compilation and validation.

### Gap/Overlap Validation for Dimensional Properties

Dimensional property values are validated to ensure that identical tables within a rules project neither overlap nor
leave gaps in the range of their possible values.

## Improvements

* Improved Decision Table validation to support enum types and automatic domain discovery based on condition rule
  values.
* Double-click opens the Table Editor in edit mode.
* Wizards for new table creation and copy operations are improved.
* Improved performance and memory allocation for Decision Table Lookup modification.
* Validation messages for "unique in: module" constraints are shown in the UI.
* An error is shown when an attribute is set more than once in a Data table.

## Bug Fixes

* Fixed: No check for free space on the left and right sides when adding properties to a table.
* Fixed: No error shown when tables share the same signature but have different return types.
* Fixed: Property values become blank when one property has an error.
* Fixed: Property section not disabled for tables of type `Data type`.
* Fixed: Checkbox not active in the table editor in IE 7 when set to `true`.
* Fixed: Memory leaks resolved.
* Fixed: Cannot add properties for a table with a vertical structure in the Table Editor.
* Fixed: Table Editor under Firefox 3 makes cells too wide.

---
title: OpenL Tablets 5.8.0 Release Notes
date: 2011-07-14
description: Rewrites the Web Services application, simplifies configuration, introduces array expression indexing,
    new utility methods (max, min, avg, sum, sort, and others), and multiple WebStudio improvements.
---

OpenL Tablets **5.8.0** is a major feature release with a rewritten Web Services application, simplified configuration,
and significant Core enhancements.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Known Issues](#known-issues)

## New Features

### Rewritten Web Services Application

The OpenL Web Services application now supports complex deployments with advanced functionality, configuration-driven
scenarios, and customization options for complex setups.

### Simplified OpenL Tools Configuration

Configuration files for all OpenL Tablets tools — WebStudio, Rules repository, Web Services application — have been
revised and simplified. Settings are stored in the `webstudio.home` directory for easier upgrades.

### Expression Indexing in Arrays

New syntax for selecting array elements matching conditions:

* `array[select first having <condition>]`
* `array[select all having <condition>]`

### New Utility Methods

New utility methods are available anywhere in rules: `max`, `min`, `avg`, `big`, `small`, `median`, `sum`, `product`,
`quotient`, `mod`, `allTrue`, `anyTrue`, `xor`, `slice`, `sort`, `contains`, `indexOf`, `out`.

## Improvements

**Core:**

* Method invocation with array arguments matched against signature.
* Array-type final arguments passable as comma-separated elements.
* `SpreadsheetResult` conversion to object lists.
* Gap and overlap analysis enhancements for partial overlaps and defaults.

**WebStudio:**

* Non-Latin URL character support.
* Application properties configurable via JVM system properties.
* Recent table access history.
* Cross-table reference display.
* Repository link when workspace is empty.
* Hidden deleted projects from tree.
* PermGen-resistant JVM defaults.
* Disabled table editing from trace screen.
* TableEditor performance improvements.
* Chrome and Firefox 5 compatibility.

**Other:**

* Eclipse: Ant task dependency manager property.
* Project migration to Maven 3.

## Bug Fixes

* Fixed: `Integer` to `String` casting failure.
* Fixed: Parser halting after the first Spreadsheet table error.
* Fixed: TBasic return type casting issues.
* Fixed: Short array declaration in TBasic.
* Fixed: Duplicate compilation in `WrapperAdjustingInstantiationStrategy`.
* Fixed: Excel file selection with duplicate filenames in WebStudio.
* Fixed: Double project reload in WebStudio.
* Fixed: Table property editing via double-click in Firefox/Chrome.
* Fixed: Java bean generation missing default values in Eclipse.
* Fixed: `getMethodCaller()` access errors in Tutorials.
* Fixed: WebDAV repository startup hang in Demo.

## Known Issues

* Gap/overlap validation for large tables may perform poorly; algorithm selection is expected in the next release.
* Eclipse: OpenL Project editing requires `-Dworkspace.local.home={$workspace_loc}` VM argument.

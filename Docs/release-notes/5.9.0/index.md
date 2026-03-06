---
title: OpenL Tablets 5.9.0 Release Notes
date: 2012-03-02
description: Introduces the new WebStudio Beta UI, Typed Spreadsheet Result, Decision Table Dispatcher, Run Tables, rounding utilities, and performance improvements. Requires Tomcat 7.
---

OpenL Tablets **5.9.0** is a feature release introducing the redesigned WebStudio UI (beta), typed spreadsheets, and
dispatching improvements. Tomcat 7 is now required.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Known Issues](#known-issues)

## New Features

### New WebStudio UI (Beta)

A redesigned AJAXian WebStudio UI with reworked navigation, executable tables with parameters, repositioned table
properties, and a new TableEditor menu.

### Typed Spreadsheet Result

Enables custom types for Spreadsheet tables at runtime.

### Decision Table Dispatcher

An additional rules dispatching approach for overloaded tables.

### Run Tables from WebStudio

Execute rules with input arguments directly without creating test tables.

## Improvements

**Core:**

* ID property for tables.
* Utility methods for rounding decimal values.

**WebStudio:**

* Enhanced test display, copy functionality, test selection in TestSuite, project filtering, performance gains,
  workspace upload feature, and user settings storage.

**Web Services:**

* Bug fixes and stabilization.

## Bug Fixes

* Fixed: Table navigation issues for dimension-overloaded tables.
* Fixed: List parameter handling in WebStudio.
* Fixed: Date handling in the Repository.
* Fixed: IE 9.0 compatibility issues.
* Fixed: Spreadsheet trace display.
* Fixed: `DoubleRanges` border inclusion logic.
* Fixed: `contains` method for `DoubleValue`.
* Fixed: Eclipse plugin installation and `NullPointerException` errors.

## Known Issues

* Integer and decimal type parsing limits.
* Excel sheet special character handling.
* Array datatype validation.
* Collection initialization limitations.
* Partial Cyrillic support.

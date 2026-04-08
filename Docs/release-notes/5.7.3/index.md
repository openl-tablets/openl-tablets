---
title: OpenL Tablets 5.7.3 Release Notes
date: 2010-11-18
description: Introduces SimpleRules and SimpleLookup tables, an execution mode with up to 5× lower memory usage,
    multi-file rule modules, and reduces debug-mode memory consumption by up to 30%. Fixes 12 bugs in the core engine and WebStudio.
---

OpenL Tablets **5.7.3** is a feature release with new table types, performance improvements, and bug fixes.

## New Features

### SimpleRules and SimpleLookup Tables

Simplified forms of Decision Table for use when input parameters are simple and the rules logic is limited to comparing
inputs against condition values and returning a single output. These tables reduce authoring complexity for
straightforward rule sets.

### Execution Mode in Engine

A new execution mode skips loading test tables and omits debug information from memory, reducing memory consumption by
up to 5× compared to debug mode. This is intended for production deployments.

### Multi-File Rules Module

The OpenL Runtime API now supports loading multiple separate rule files as a single rules module instance.

## Improvements

* Memory consumption decreased by up to 30% in debug mode.
* Datatypes are displayed in business view.
* Added links to test tables on "Results of running" screens.
* Updated Datatype wizard to support alias datatypes.
* Changed internal table processing logic and table API inside the OpenL engine.

## Bug Fixes

* Fixed: Explicit conversion does not work (except `double` → `int` and `long` → `int`).
* Fixed: Upcasts do not work (except `char` → `int`; `int` → `double`, `long`; `long` → `double`).
* Fixed: Extendable datatype is not imported during static generation.
* Fixed: Property table parsed incorrectly when a property name is defined incorrectly.
* Fixed: Error displayed when a Decision Table contains a field with an alias datatype.
* Fixed: `JavaWrapperGenerator` crashes when the method header is malformed.
* Fixed: Internal imports are not added when there is no import statement in the module.
* Fixed: Users can run test cases that contain errors.
* Fixed: Technical column names displayed in the test results table instead of user-defined names.
* Fixed: Files with the `.xlsm` extension are not displayed in the Rule Editor.
* Fixed: Empty trace window displayed when a rule table contains an error.
* Fixed: Cannot compare versions in the repository when a project was created from a single Excel file.

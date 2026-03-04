---
title: OpenL Tablets 5.15.0 Release Notes
date: 2015-04-17
description: Introduces redesigned condition indexing for lower RAM usage and faster compilation,
    WADL schema support for REST services, core memory optimization, and 5 bug fixes.
---

OpenL Tablets **5.15.0** is a feature release introducing condition indexing improvements, WADL schema generation, and
significant memory optimizations.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## New Features

### Condition Indexing

A new, more robust and less RAM-consuming implementation of indexing conditions in Decision Tables:

* 2-dimensional lookups now use 2 indexes instead of one.
* New condition column header (`MC`) for merged condition rows.
* Improved parsing algorithm to decrease compilation time.

### WADL Schema Support

The Web Services application now generates a full request schema for REST services, with comprehensive documentation of
argument types, field names, and enumerated lists, enabling service auto-discovery.

### Core Memory Optimization

Several internal implementation parts were improved to reduce RAM consumption and compile rules faster.

## Improvements

**Core:**

* Dispatching algorithm enhanced: blank business dimension properties now match any values.
* Improved performance and parsing for ranges.

**WebStudio:**

* Eliminated `rules.xml` creation after module deletion from projects.

**Web Services:**

* Selected modules initialize at startup when using lazy loading.
* REST parameter names transformation standardized.
* Module name mismatch warnings suppressed per request.

**Other:**

* Added `rules-deploy.xml` to examples and tutorials.
* Added WAR building for WebSphere.
* Updated CXF to 3.0.3 in Web Services; added CXF 2.7 in databinding.

## Bug Fixes

* Fixed: Compilation of OpenL Tablets source code under Linux.
* Fixed: `getValue()` function.
* Fixed: Casting from `CustomSpreadsheetResult[]` to `SpreadsheetResult[]`.
* Fixed: Variation arguments display — `arg0`, `arg1` now show actual parameter names.
* Fixed: Datatype naming restrictions for reserved Java class names.

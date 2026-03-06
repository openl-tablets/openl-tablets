---
title: OpenL Tablets 5.16.0 Release Notes
date: 2015-09-14
description: Introduces Origin business dimension property, repository deployer utility, RMI publisher, Java 8 support,
    enhanced dispatching performance, and 7 bug fixes.
---

OpenL Tablets **5.16.0** is a feature release introducing the Origin business dimension, Java 8 support, an RMI
publisher, and performance improvements.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## New Features

### Origin Business Dimension Property

A new business dimension property called `Origin` offers two options: `Base` and `Deviation`. This enables building
hierarchies of broader and more targeted rules:

* Rules marked as `Deviation` supersede those marked `Base` or unmarked.
* Rules marked as `Base` take precedence over unmarked rules.

### Repository Deployer

A new utility class facilitates deploying OpenL projects to production repositories (DB, RMI, WebDAV, and others).

## Improvements

**Core:**

* Enhanced dispatching performance between rule versions.
* Support added for array index operators on `SpreadsheetResult` arrays.
* Boxing capability for primitive value types.
* `_DEFAULT_` keyword now supported in Datatype code generation via the OpenL Maven Plugin.
* Complete removal of Word support.
* CW value support added for the US State property.
* Region and Province properties renamed to Canada Region and Canada Province.

**WebStudio:**

* Improved performance for Explanation and Trace functionality.

**Web Services:**

* New RMI publisher enables deploying rules projects as RMI endpoints.
* Support for projects with spaces in URLs within `rules-deploy.xml` for REST services.
* Generated WADL now includes default values.
* Enhanced JSON databinding with variation support.

**Demo Package:**

* Modified Java options and port configuration in `setenv.bat`.

**Other:**

* Java 8 support added.
* `rules-deploy.xml` removed from all examples and tutorials.
* Upgraded POI to 3.12.
* Updated CXF to 3.0.5 (Web Services only).

## Bug Fixes

* Fixed: Parsing of empty values in Alias Datatype.
* Fixed: Auto Type Discovery property issues.
* Fixed: Memory leaks in Run and Test tables.
* Fixed: Concurrent test execution in TBasic tables.
* Fixed: Test table precision not applied consistently across result columns.
* Fixed: Broken Datatype links with spaces.
* Fixed: `BigDecimal` calculation issues.

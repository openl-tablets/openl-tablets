---
title: OpenL Tablets 3.1.0.7 Release Notes
date: 2006-10-27
description: Introduces a Web Studio prototype, Decision Table return columns, Environment Table package imports,
    Data Table array support, programmatic version access, two new tutorials, and deployment documentation.
    Simplifies configuration by making Ant, properties, and lang.config optional.
---

OpenL Tablets **3.1.0.7** is a major release introducing the Web Studio prototype and significant configuration
simplifications.

## New Features

### Web Studio Prototype

A prototype of the OpenL Tablets Web Studio is now available, providing a browser-based interface for working with rules
projects.

### Decision Table Return Columns

Decision Tables now support return types other than `void` using new `RET` (return) columns, enabling rules to produce
typed output values.

### Environment Table Import Field

A new import field in the Environment table supports managing package imports as an alternative to properties files.

### Data Arrays of Primitive Types

Data tables now support arrays of primitive types.

### Programmatic Version Access

The methods `org.openl.main.OpenLVersion.getVersion()` and `org.openl.main.OpenLVersion.getBuild()` are now available
for retrieving version and build information programmatically.

### New Tutorials

* **Tutorial 2** — Introduction to Data Tables.
* **Tutorial 3** — Advanced Data and Decision Tables.

### OpenL Tablets Deployment Documentation

New documentation covering OpenL Tablets deployment scenarios and configuration.

## Improvements

* Documentation updated with a Web Studio manual and screenshots.
* Main modules now display version and build information at startup.
* Configuration is simplified: Ant-based and property-based configuration are now completely optional.
* The `lang.config` project is no longer required.
* The `org.openl.rules.java.ant.properties` file is no longer required.
* The language configuration field is now optional and defaults to `org.openl.rules.java`.
* The `Configuration Project` has been removed as obsolete.
* Unnecessary JARs have been removed from the build.
* DT API now exposes more structural information.
* Existing examples updated to reflect the simplified configuration mechanism.
* Step-by-step self-documented tutorial examples created.

## Bug Fixes

* Fixed: Error in `for` loop execution.
* Fixed: `OpenLTool.makeOpenLMethod()` now properly validates the return value type.

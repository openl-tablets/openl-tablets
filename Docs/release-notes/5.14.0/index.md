---
title: OpenL Tablets 5.14.0 Release Notes
date: 2015-01-22
description: Reworks RESTful services, adds database production repository, module copy UI in WebStudio,
    multithreaded variations, new range syntax, Internet Explorer 11 support, and 8 bug fixes.
---

OpenL Tablets **5.14.0** is a feature release with significant improvements to RESTful services, production repository
options, and WebStudio usability.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Deprecations](#deprecations)

## New Features

### RESTful Services

RESTful services support has been significantly reworked to enable all rule features and much easier configuration. All
rule types and input/output combinations are now supported without requiring customizations.

### Database for Rules Production Repository

The Rules Production Repository can now be configured to store content in various databases via JDBC drivers. The system
can automatically create the required schema if tables do not exist.

### Copy Module in WebStudio

WebStudio provides a new UI for copying modules, with a dialog showing module-specific details and validating naming
against versioning patterns.

## Improvements

**Core:**

* Added multithreaded calculation for rules variations.
* Enhanced indexed conditions in Decision Table with improved relativity operations indexing.
* Enhanced auto casts, including implicit conversions between `String` and numeric types.
* Added new Ranges syntax: `>=3 <=5`.

**WebStudio:**

* Added RESTful services for Rules Repository access.
* Added Internet Explorer 11 support.
* Added repository configuration step to the Installation Wizard.
* Added cross-table links for datatypes and data tables.
* Added active tab highlighting.
* Added file name mismatch warnings during module updates.
* Added option to remove files when deleting modules.
* Added RESTful/SOAP services deployment configuration UI.
* Added service name and class validation.
* Added Environment table dependency highlighting.
* Added backward compatibility support for different OpenL versions.

**Web Services:**

* Added deployments and versioning support to the File System Data Source.

**Other:**

* Moved Jackrabbit properties into `remote-repository-server.war`.
* Removed dependency on `commons-logging` — OpenL Tablets now uses SLF4J.

## Bug Fixes

* Fixed: Missing implicit casts for certain Java types to OpenL Value types.
* Fixed: Incorrect Trace display in Decision Tables with action columns.
* Fixed: Web Services deadlocking preventing project redeployment.
* Fixed: Test execution blocked when tested rules contain errors.
* Fixed: Null pointer errors from data table values.
* Fixed: Module updates failing with circular datatype dependencies.
* Fixed: `round()` function does not tolerate `null` values.
* Fixed: Null pointer errors from unparseable datatype table cells.

## Deprecations

| Deprecated Item                            | Notes                             |
|:-------------------------------------------|:----------------------------------|
| `id` tag in `rules.xml` project descriptor | Use alternative identifier fields |

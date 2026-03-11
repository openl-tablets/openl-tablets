---
title: OpenL Tablets 5.17.0 Release Notes
date: 2016-03-05
description: Introduces multiple service versions, wildcard module dependencies, Activiti BPM integration,
    web service request/response logging to Cassandra, and 7 bug fixes.
---

OpenL Tablets **5.17.0** is a feature release introducing multiple service version support, BPM integration, and service
logging capabilities.

## New Features

### Multiple Service Versions in Web Services

Deploy and call multiple versions of the same service simultaneously. Version numbers are displayed in brackets on the
Web Services page.

### Wildcards Support in Module Dependencies

Define dependency modules using wildcard patterns in the Environment table for automatic loading.

### Activiti BPM Integration

New integration artifacts enable OpenL Tablets services to be accessed through BPM tasks and expressions.

### Storage for Web Services Log

Service requests and responses can now be stored in a database. Apache Cassandra is the default option.

## Improvements

**Core:**

* Enhanced relationship specification between data tables with the ability to reference specific data values.
* Empty cells in Data and Test tables now return `null` instead of empty collections.
* Added package name support in full Java class names.
* Introduced `instanceOf(object, Class)` operator.

**WebStudio:**

* Rule return types now display in editor hints.
* Optimized module load time by skipping dependency reloads after editing.
* Added dispatcher table for single rule tables overloaded by business dimension.
* Improved Trace tree load performance.
* Drop-down list support for cell values in Simple Rules tables.

**Web Services:**

* Script-based project deployer added (database deployment without UI).
* Enhanced WADL descriptions for `Date` type default values.
* Default database settings added to production repository properties.

**Other:**

* Demo package project revisions changed from 0 to 1.
* Upgraded Commons Collections to 3.2.2.
* Removed `commons-beanutils` and `commons-io` dependencies from Core.
* Upgraded POI to 3.11.

## Bug Fixes

* Fixed: Web Services startup issue with JNDI settings under WebSphere.
* Fixed: Production repository database schema creation failure with JNDI.
* Fixed: DataType field link references in WebStudio.
* Fixed: Module loading errors with incorrect default values in overloaded rules.
* Fixed: Empty expected result display in Test tables.
* Fixed: Simple Rules functionality with array return values.
* Fixed: Empty business dimension property handling in overloaded rules.

## Breaking Changes

### Empty Cell Behavior Changed

Empty cells in Data and Test tables now return `null` instead of empty collections. Review any rules or tests relying on
empty collections from blank cells.

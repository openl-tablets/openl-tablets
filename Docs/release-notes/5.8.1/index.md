---
title: OpenL Tablets 5.8.1 Release Notes
date: 2011-11-03
description: Introduces lazy module loading in web services, null support in arithmetic operations,
    range expressions in Spreadsheets, Internet Explorer 9 support, and migration to JSF2/RichFaces 4.
---

OpenL Tablets **5.8.1** is a feature release with performance improvements, new arithmetic capabilities, and a migration
to JSF2 and RichFaces 4.

## New Features

### Lazy Loading of Modules in Web Services

Increased web service performance for projects with a large number of modules. Modules load on demand and are unloaded
when rarely used.

### Null Values in Arithmetic Operations

Added support for `+`, `-`, `*`, `/` operations on `null` values for custom OpenL number types. `null` is treated as `0`
for addition and subtraction, and as `1` for multiplication and division.

### Range Expressions in Spreadsheets

Range syntax `$Col1$Row1:$Col2$Row2` enables expressions such as `sum($Step_Claim_Crdb:$Step_Vacancy)` instead of
summing cells individually.

## Improvements

* Start/end request dates added to dimension properties; request date added to `RuntimeContext`.
* Currency and Language properties relocated to dimension properties.
* Web services: `RuleService` Frontend Configuration file introduced.
* Web services: Versioning support between deployment versions.
* Full Internet Explorer 9.0 support.
* Migrated to JSF2 and RichFaces 4.

## Bug Fixes

* Fixed: `slice(byte[] values, int startIndexInclusive, int endIndexExclusive)` definition unsupported.
* Fixed: Missing trace/run/test buttons for dependency module tests.
* Fixed: Module revert returns incorrect state after multiple reverts.
* Fixed: IE 9.0 color scheme application causes malfunction.
* Fixed: Dependency projects corrupted after table copy in IE.
* Fixed: `StartStudio.bat` launch failure in Eclipse.
* Fixed: Simple OpenL project WebStudio startup failure in Eclipse.
* Fixed: Descriptor addition to deployment projects blocked in the Repository.
* Fixed: Chrome redeployment window hanging in the Repository.
* Fixed: Post check-in changes rejected after project closure in the Repository.

## Known Issues

* WebStudio: Links to other tables are not clickable for tables overloaded by dimension properties.

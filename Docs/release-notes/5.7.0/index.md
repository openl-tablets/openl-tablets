---
title: OpenL Tablets 5.7.0 Release Notes
date: 2010-06-17
description: Moves all CXF configurations to Spring, repackages OpenL Tablets for Maven Central, and enables Java class
    generation from Datatype components. Significantly improves compilation performance, adds merged-cell support,
    comma-separated arrays, and fixes 12 bugs.
---

OpenL Tablets **5.7.0** is a major release with a configurable service frontend, repackaging for Maven Central, and
Datatype-to-Java generation.

## New Features

### Configurable Service Frontend

All configurations for CXF-based services have been moved to Spring. The CXF-based service frontend is also packaged as
a configurable WAR file with restructured, more flexible service infrastructure components.

### Repackaged OpenL Tablets

Utility and helper projects have been reviewed and reorganized. Eclipse library projects for all modules have been moved
to the Eclipse module. The delivery is prepared for publication to Maven Central repository.

### Datatype Usage in Java Code and Web Services

The engine generates Java classes at runtime for types defined using the Datatype component. An Ant goal enables
generating these Java files to the file system for use in Java code and web services.

## Improvements

* OpenL Rules compilation performance significantly increased.
* Types defined using the Datatype component are accessible via the Runtime API.
* Local parameter declarations in a Decision Table can be omitted for simple cases.
* Wizard for the Datatype component added to WebStudio.
* Merged cell support added for Decision Table lookup modification.
* Merged regions in rules are treated with value propagation to all rows.
* Support for comma-separated arrays in Decision Tables.
* Array editor added to the Table Editor.
* `SpreadsheetResult` is now a simple bean.
* The `failOnMiss` property is available at category and module levels.
* Test table link always displayed when the target table has associated tests.
* Engine Factory instantiation mechanism added without `IRuntimeEnv` usage.
* Static wrapper instantiation mechanism added without `IRuntimeEnv` usage.
* Helper methods for arrays: `contains` and `containsAll` added.
* "Quick tests creation" button added in WebStudio.

## Bug Fixes

* Fixed: Comparison operations (`==`, `>`, `<=`) for `short` and `float` types.
* Fixed: Accuracy for `double` values in test results.
* Fixed: Numeric editor functionality.
* Fixed: Error messages now appear in the Problems section.
* Fixed: Table editor functionality with merged cells.
* Fixed: Decision Table sorting by version when validation errors are present.
* Fixed: Property enum error display.
* Fixed: Empty errors for undefined Decision Table variables.
* Fixed: Trace functionality on first opening.
* Fixed: Tags property special symbol display.
* Fixed: Type property preservation after editing.
* Fixed: Application crash during Advanced Search on lookup tables.

## Known Issues

* WebStudio login error appears on initial access; refresh the root page (`/webstudio/`) to resolve. Adding
  `spring-web-2.0.6.jar` to the classpath provides a permanent fix.

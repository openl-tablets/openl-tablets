---
title: OpenL Tablets 5.21.2 Release Notes
date: 2018-03-28
description: Patch release with 20 bug fixes across Core, WebStudio, Rule Service, and Maven plugin,
    addressing memory leaks, Decision Table compatibility, and SOAP validation issues.
---

OpenL Tablets **5.21.2** is a patch release containing bug fixes. All demonstration examples have been converted to XLSX
format.

## Bug Fixes

**Core:**

* Fixed: Decision Tables are incompatible with expression-based condition values.
* Fixed: Null-safety mechanism failure affecting primitive return types.
* Fixed: Memory leak in Variation functionality.
* Fixed: Memory leak in the case rules recompilation process.

**WebStudio:**

* Fixed: "Compare Excel files" feature required unauthorized access permissions.
* Fixed: "Open Excel File" functionality is incompatible with the Java security manager.
* Fixed: Project version deployment configuration access issue.
* Fixed: Discarding table changes causes a link display failure.
* Fixed: Login page error message caching problem.
* Fixed: "Auto Deploy" screen is unresponsive in Internet Explorer 11.
* Fixed: Duplicate categories in "By Category Detailed" and "By Category Inversed" views.
* Fixed: Switching editor sections causes loss of unsaved changes.
* Fixed: Single-digit cell entry restriction in empty fields.
* Fixed: Private secret key updates are not being applied.

**Rule Service:**

* Fixed: Swagger API does not reflect rule changes.
* Fixed: SOAP v1.1 validation failure when using `char`/`Character` type.
* Fixed: `CXF ClientProxyFactoryBean` `NullPointerException` caused by a malformed WSDL scheme.
* Fixed: `NullPointerException` triggered by empty REST request parameters.

**Maven Plugin:**

* Fixed: Memory leak in test goal execution.

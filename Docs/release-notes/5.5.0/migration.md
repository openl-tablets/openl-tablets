---
title: OpenL Tablets 5.5.0 Migration Notes
---

## OpenL Object Cache

The cache of OpenL objects is reused even when a new user classloader is provided. Reset the cache whenever a
classloader change is required. A reload button is available in OpenL WebStudio for this purpose.

## SpreadsheetResult Package Change

The `SpreadsheetResult` class has been relocated to the package `org.openl.rules.calc.result`. Update any import
statements referencing the old package location.

## Compilation Error Handling

Compilation errors are captured differently than in previous releases. Review any code that depends on the previous
error-capture mechanism.

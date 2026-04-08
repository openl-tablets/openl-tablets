---
title: OpenL Tablets 5.17.2 Release Notes
date: 2016-04-27
description: Introduces deployment group property for shared repositories, updated rounding behavior for Double
    with new roundStrict() variants, and simplified CI workflow with JAR-based rules deployment.
---

OpenL Tablets **5.17.2** is a patch release with improvements to deployment configuration and rounding behavior.

## Improvements

### Deployment Configuration Group Property

Introduced a `group` property in deployment configuration, enabling the same production repository to be shared between
differently configured instances with selective rules project exposure. Separate repositories remain the preferred
approach.

### ModeShape Logging

Deprecated ModeShape warning log messages are now suppressed while deploying projects to a database repository.

### Rounding Functions for Double

Modified `round(value)` and `round(value, scale)` functions for `Double` to align with `DoubleValue` behavior per the
IEEE 754 specification. Added `roundStrict(value)` and `roundStrict(value, scale)` variants that preserve the legacy
behavior by disregarding Unit of Least Precision (ULP) arithmetic inaccuracies.

### Simplified CI Workflow

Introduced a streamlined development process allowing rules projects to be packaged as JAR files inside Web Services,
with automatic deployment during startup.

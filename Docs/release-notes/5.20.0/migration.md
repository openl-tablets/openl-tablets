---
title: OpenL Tablets 5.20.0 Migration Notes
---

## Deployment Repository — Version in Deployment Name

The default value of `version-in-deployment-name` has changed from `true` to `false`. Verify your deployment
configuration and update if needed.

## Java Version Requirements

* **Minimum**: Java 7
* Java 9 is now supported

## OpenL Rules — Implicit Double to Integer Casting Removed

The implicit casting from `Double` to `Integer` has been removed. Update any rules that relied on this behavior to use
explicit casting.

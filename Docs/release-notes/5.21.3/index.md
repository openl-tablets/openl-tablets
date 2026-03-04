---
title: OpenL Tablets 5.21.3 Release Notes
date: 2018-04-03
description: Adds autocast from single values to arrays and Range types, null-safety for array index access,
    4 bug fixes, and deprecates the @VariationsFromRules annotation.
---

OpenL Tablets **5.21.3** is a patch release with improvements, bug fixes, and a deprecation notice.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Deprecations](#deprecations)

## Improvements

**Core:**

* Autocast is now supported from a single value to an array.
* Autocast is now supported from a single value to a Range type.
* Null-safety is now supported when referring to an array by index.

## Bug Fixes

**Core:**

* Fixed: An incorrect method is called for arrays and single values.
* Fixed: Varargs functionality depends on execution environment.
* Fixed: Decision Tables do not support Ranges.

**WebStudio:**

* Fixed: An error occurs on switching between dependent modules in "Single Module" mode.

## Deprecations

| Deprecated Item                   | Replacement | Removal Version |
|:----------------------------------|:------------|:----------------|
| `@VariationsFromRules` annotation | —           | 5.22.0          |
| `VariationDescription` class      | —           | 5.22.0          |

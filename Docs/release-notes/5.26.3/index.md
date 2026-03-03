---
title: OpenL Tablets 5.26.3 Release Notes
date: 2022-10-31
description: Custom error bodies for user exceptions in Rule Services, VariationPack deprecation,
    bug fixes, and Jackson library update.
---

OpenL Tablets **5.26.3** is a maintenance release introducing custom error bodies for user exceptions in Rule Services, deprecating VariationPack, and fixing several bugs.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Deprecations](#deprecations)
* [Library Updates](#library-updates)

## Improvements

### Rule Services

* Custom error bodies for user exceptions are now supported in OpenL Tablets Rule Services.

## Bug Fixes

### Core

* Fixed a property file not being read from the dependent project.
* Fixed no warning message being displayed if the `_res_` column is missing in the test table.

### Rule Services

* Fixed events in logs appearing in incorrect chronological sequence if logging is enabled.
* Fixed properties files not being read and localization not working if a project is deployed from zip deployment.

## Deprecations

* `VariationPack` has been deprecated.

## Library Updates

| Library  | Version  |
|:---------|:---------|
| Jackson  | 2.13.4.1 |

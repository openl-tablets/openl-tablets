---
title: OpenL Tablets 5.21.10.1 Release Notes
date: 2018-12-06
description: Introduces the new like() pattern matching function and fixes 10 bugs across Maven plugin, Core SmartRules,
    WebStudio datatypes, and web services deployment. Replaces the faulty 5.21.10 build.
---

OpenL Tablets **5.21.10.1** replaces the faulty 5.21.10 build. It introduces a new pattern matching function and
includes bug fixes.

> **Note:** Build 5.21.10 is faulty. Use OpenL Tablets 5.21.10.1 instead.

## Contents

* [New Features](#new-features)
* [Bug Fixes](#bug-fixes)

## New Features

### `like()` Pattern Matching Function

A new `like()` function has been introduced to replace complex RegExp pattern operations:

* `#` matches any digit character
* `@` matches any letter character

**Examples:**

* `like("(541) 754-3010", "(###) ###-####")` returns `TRUE`
* `like("Mike Gordon", "@+ @+")` returns `TRUE`
* `like("123-ABC", "#-#-@")` returns `FALSE`

## Bug Fixes

**Maven Plugin:**

* Fixed: Build intermittently fails when executing in parallel mode.

**Core:**

* Fixed: SmartRules occasionally displays "There is no index X in the sequence" errors.
* Fixed: SmartRules incorrectly matches columns for compound return types.
* Fixed: `copy()` functionality fails with Array parameters.
* Fixed: Tests for `copy()` function on `List` datatype generate errors.
* Fixed: Calculating max and min operations fails when parameters belong to different explanation types.

**WebStudio:**

* Fixed: "Datatype validation is failed..." message appears during datatype updates in projects with dependencies.
* Fixed: `java.lang.IllegalArgumentException` occurs during testing when two dependent modules share identical aliases
  in separate datatype packages.

**Web Services:**

* Fixed: Deployment halts when projects contain the property `lazy-modules-for-compilation name=*`.
* Fixed: Projects deploy successfully despite incorrect `serviceClass` method return types.
* Fixed: Web service integration with AWS S3 repository fails due to missing configuration properties.

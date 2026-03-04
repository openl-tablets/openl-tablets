---
title: OpenL Tablets 5.19.4 Release Notes
date: 2017-07-11
description: Adds MariaDB/PostgreSQL authentication support, Oracle 12c and Amazon Aurora database compatibility,
    null-safe arithmetic operations, and 6 bug fixes.
---

OpenL Tablets **5.19.4** is a patch release with new database support, improvements, and bug fixes.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## New Features

* Support added for MariaDB and PostgreSQL databases for authentication.

## Improvements

**Core:**

* Arithmetic operations are now null-safe: `null` equals zero in addition and subtraction, and equals one in
  multiplication and division.

**Demo Package:**

* Dynamic client implementation replacing the static approach.

**Maven Plugin:**

* Beans retrieval capability from previous builds when the `clean` goal is omitted.
* Zipped OpenL artifact attachment without a classifier when no conflicts exist.

**Repository:**

* Oracle 12c support added.
* MariaDB and Amazon Aurora database support for the Deployment Repository.
* REST-based project upload capability to the Design Repository.

## Bug Fixes

* Fixed: Incorrect behavior of Admin Groups.
* Fixed: International character username display issue.
* Fixed: Java types not added to OpenL value types for explicit casts.
* Fixed: Multi-user/Production mode configuration issue.
* Fixed: Incorrect behavior of the `round()` method.
* Fixed: Incorrect behavior of the Deploy Configuration "Lock" functionality.

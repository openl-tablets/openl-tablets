---
title: OpenL Tablets 5.1.1 Release Notes
date: 2008-12-11
description: Introduces Decision Table consistency checking for under-coverage and overlapping,
    user-defined repository tags, BigDecimal and BigInteger arithmetic, and Maven 2 build integration.
    Fixes 2 bugs in the Table Editor and Decision Table formula handling.
---

OpenL Tablets **5.1.1** is a feature release with new validation capabilities and language improvements.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## New Features

### Decision Table Consistency Checking

Decision Tables are now validated for under-coverage (missing combinations of input values) and overlapping between
rules, helping authors identify logical gaps and conflicts in rule sets.

### User-Defined Repository Tags

The OpenL Repository now supports user-defined tags for organizing and classifying projects.

## Improvements

* The OpenL language now supports `BigDecimal` and `BigInteger` arithmetic.
* The build process has been streamlined to work with Maven 2.
* Data Tables now allow entering a fake primary key for types that do not have a primary key field.

## Bug Fixes

* Fixed: Firefox 3 inserts an extra incorrect symbol in the Table Editor.
* Fixed: Bug in Decision Table for formulas of the form `=1`.

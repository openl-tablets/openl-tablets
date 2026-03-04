---
title: OpenL Tablets 5.22.6 Release Notes
date: 2019-10-16
description: Maintenance release adding the length() function for Map/List/Set objects, Tracer improvements,
    3 bug fixes, and library updates.
---

OpenL Tablets **5.22.6** is a maintenance release containing improvements, bug fixes, and library updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

**Core:**

* `length()` function is added for `Map`, `List`, and `Set` objects.

**Rule Services:**

* Tracer functionality is improved to reduce temporary array creation.
* Jackson is updated to v2.10.

## Bug Fixes

**WebStudio:**

* Fixed: No error message is displayed when running versioned rules without a runtime context if the number of business
  versions is even.

**Rule Services:**

* Fixed: Multiple allocation failures in JVM.

**Core:**

* Fixed: Errors on opening a Simple Rules table that contains an array defined via merged columns.

## Library Updates

| Library           | Version |
|:------------------|:--------|
| Jackson           | 2.10    |
| Commons BeanUtils | 1.9.4   |
| Commons Compress  | 1.19    |

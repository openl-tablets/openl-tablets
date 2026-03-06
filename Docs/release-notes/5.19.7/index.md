---
title: OpenL Tablets 5.19.7 Release Notes
date: 2017-10-12
description: Adds Java 9 support, isNotEmpty() function, null-safety for comparison operators, improved repository scheduling, and a fix for allTrue() and anyTrue() functions.
---

OpenL Tablets **5.19.7** is a patch release with new features, improvements, and a bug fix.

## New Features

**Core:**

* Java 9 is now supported.

## Improvements

**Core:**

* Added `isNotEmpty()` function as a more readable alternative to `!isEmpty()`.
* Null-safety support added for all comparison and logical operators.

**Repository:**

* Replaced `Timer` with `ScheduledThreadPoolExecutor` to avoid issues with changing system time.

## Bug Fixes

* Fixed: Issue with `allTrue()` and `anyTrue()` functions.

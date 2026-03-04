---
title: OpenL Tablets 5.20.3 Release Notes
date: 2018-05-25
description: Adds auto-configuration for Java version and memory settings, improves Maven plugin error messaging,
    and fixes 4 bugs in Core multithreading, Maven plugin, Repository, and WebStudio.
---

OpenL Tablets **5.20.3** is a patch release with auto-configuration improvements and bug fixes.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## New Features

* Auto-configuration has been added to automatically adjust settings for different Java versions and memory sizes. Users
  can now install Java and run the start command without manual configuration in most cases.

## Improvements

* The OpenL Maven Plugin received enhancements to its error messaging when Spreadsheet comparisons fail.

## Bug Fixes

**Core:**

* Fixed: Multithreading execution problems with TBasic.
* Fixed: Improper static field access functionality.

**Maven Plugin:**

* Fixed: Memory leak.

**Repository:**

* Fixed: File deployment failures to the database repository due to JDBC pool management.

**WebStudio:**

* Fixed: Deployment configuration deletions are not properly reflected in active deployments.

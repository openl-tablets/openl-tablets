---
title: OpenL Tablets 5.21.4 Release Notes
date: 2018-06-14
description: Adds multi-application repository deployment filtering, JUnit4 test reports for Maven plugin,
    Jetty support in Rule Service, and 10 bug fixes for WebStudio, Rule Service, and Core.
---

OpenL Tablets **5.21.4** introduces multi-application repository support and includes improvements and bug fixes.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## New Features

### Multi-Application Repository Support

The system enables setting up a Deployment Filter to manage deployments from shared datasource repositories across
multiple applications, filtering by deployment name.

## Improvements

**Demo:**

* Support for 32-bit Java (x86) in DEMO.

**Rule Service:**

* Swagger, CXF, and Jackson libraries updated.
* OpenL version and libraries in the classpath are displayed in the log.
* Support for Jetty.

**Maven Plugin:**

* OpenL test reports are now generated in JUnit4 format.

## Bug Fixes

**WebStudio:**

* Fixed: ZIP files are corrupted on project export.
* Fixed: Application incompatibility with MySQL 8.0.11.
* Fixed: Assign operation returns the right side of expression instead of the left side.
* Fixed: `StringIndexOutOfBoundsException` when clicking a Spreadsheet with a field used as a function.

**Rule Service:**

* Fixed: WADL returns duplicated elements in complex types.
* Fixed: REST services incompatibility with capitalized parameters.

**Core:**

* Fixed: `ArrayCast` throws `ArrayIndexOutOfBoundException`.
* Fixed: Index operations fail with `NullPointerException` for non-aggregable types.
* Fixed: Decision Table range indexes bug.
* Fixed: Test tables incorrectly support business dimensional properties.

## Library Updates

| Library    | Version |
|:-----------|:--------|
| CXF        | 3.1.15  |
| Jackson    | 2.9.5   |
| Swagger    | 1.5.19  |
| Swagger UI | 3.14.0  |

---
title: OpenL Tablets 5.23.6 Release Notes
date: 2020-09-17
description: Release with Java 14 support, JCache API integration, multiple file name patterns, SmartRules
    and SpreadsheetResult fixes, and library updates.
---

OpenL Tablets **5.23.6** includes improvements, bug fixes, and library updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

**WebStudio:**

* The `Implementation-Version` field is now added to the generated `MANIFEST.MF`.
* Multiple file name patterns are now supported.
* Tutorial 8 featuring SmartRules examples is included.

**Core:**

* Java 14 is now supported.

**OpenL Maven Plugin:**

* Flat-structured OpenL projects are now supported.

**Rule Services:**

* JCache API (JSR-107) integration reduces the hard dependency on cache providers.

## Bug Fixes

**WebStudio:**

* Fixed: Corrupted UI for the "Copy Module" pop-up in Firefox.

**Rule Services:**

* Fixed: The `@class` parameter requirement in REST requests for top-level classes.

**Core:**

* Fixed: SmartRules creates empty objects when return values are undefined.
* Fixed: `USE_DEFAULT` does not function correctly for nillable Datatype fields.
* Fixed: `IllegalArgumentException` at runtime when deserializing `SpreadsheetResult` from JSON.
* Fixed: `SpreadsheetResult.class` compilation error.

## Library Updates

| Library          | Version              |
|:-----------------|:---------------------|
| Spring Framework | 5.2.8.RELEASE        |
| Spring Security  | 5.3.4.RELEASE        |
| CXF              | 3.3.7                |
| Jackson          | 2.11.2               |
| Jetty            | 9.4.31.v20200723     |
| Tomcat           | 9.0.37               |
| Elasticsearch    | 6.8.12               |
| JGit             | 5.8.1.202007141445-r |
| Swagger UI       | 3.31.1               |
| Swagger          | 1.6.2                |
| JCodeModel       | 3.4.0                |
| HttpClient       | 4.5.12               |
| cache2k          | 1.2.4.Final          |
| Commons Lang     | 3.11                 |
| Commons IO       | 2.7                  |

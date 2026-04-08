---
title: OpenL Tablets 5.21.5 Release Notes
date: 2018-07-19
description: Reduces SpreadsheetResult footprint in Rule Service, improves WebStudio performance,
    and fixes 11 bugs in WebStudio, Core, and Docker deployments.
---

OpenL Tablets **5.21.5** is a patch release with performance improvements and bug fixes.

## Improvements

* Reduced footprint of `SpreadsheetResult` in Rule Service.
* Added the ability to exclude logging of specific methods in the Rule Service interface.
* Improved OpenL WebStudio performance.

## Bug Fixes

**WebStudio:**

* Fixed: Project cannot be opened if it contains a table with a method beginning with an equal sign (`=`).
* Fixed: Project cannot be opened if it has a datatype that uses an undefined datatype in a Spreadsheet.
* Fixed: Incorrect error message presented when incorrect types are returned.
* Fixed: Docker — "Run into file" and "Test into file" functionality does not work.
* Fixed: Docker — WebStudio hangs on export.

**Core:**

* Fixed: Comma-separated values do not function in Data and Test tables for `List` values.
* Fixed: Method `toString` not found for `Set` interface.
* Fixed: Empty error displayed in Spreadsheet when a method with multiple business versions is called.
* Fixed: Varargs method binding fails with Spreadsheet Auto Type Discovery enabled.
* Fixed: Local time in REST requests is adjusted by the UTC timezone.
* Fixed: Performance issue in `ObjectToDataOpenCastConvertor.getConvertor` method.

## Library Updates

| Library          | Version        |
|:-----------------|:---------------|
| Spring Framework | 4.3.18.RELEASE |
| XStream          | 1.4.10         |

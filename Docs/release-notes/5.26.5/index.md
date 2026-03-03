---
title: OpenL Tablets 5.26.5 Release Notes
date: 2023-02-02
description: OpenAPI validation in Maven plugin, multiple bug fixes for Range Editor, trace display,
    Unicode support, and library updates.
---

OpenL Tablets **5.26.5** is a maintenance release introducing OpenAPI validation in the Maven plugin, multiple bug fixes across WebStudio, Rule Services, and DEMO, and library updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

### Maven Plugin

* OpenAPI validation in the OpenL Maven plugin is supported.

## Bug Fixes

### WebStudio

* Fixed Range Editor failing to open for spreadsheet tables with `IntRange` or `DoubleRange` result types.
* Fixed Trace displaying values differently between the tree and returned results tabs.
* Fixed `NullPointerException` error appearing in null-safety test results for the Intersection method.
* Fixed constructor tooltips displaying incorrect information.
* Fixed range format showing incorrectly in returned objects.
* Fixed negative values not being enterable in Range Editor.

### Demo

* Fixed runtime permission error occurring when invoking the `copy()` method in OpenL Tablets Rule Services.

### Rule Services, WebStudio

* Fixed dependency project properties file messages not being overridden by main project properties.
* Fixed Unicode characters not being supported properly in OpenL.

### Maven Plugin

* Fixed the `verify` goal failing because of duplicate dependencies in Maven plugin classloader.

## Library Updates

| Library                | Version      |
|:-----------------------|:-------------|
| Spring Framework       | 5.3.25       |
| Spring Boot            | 2.7.8        |
| Ant                    | 1.10.13      |
| Netty                  | 4.1.87.Final |
| Reactor Netty          | 1.1.2        |
| Jackson                | 2.14.2       |
| Amazon AWS SDK         | 1.12.395     |
| Azure Blob Storage SDK | 12.20.2      |
| MSSQL Driver           | 11.2.3.jre11 |
| AspectJ                | 1.9.19       |

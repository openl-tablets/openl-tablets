---
title: OpenL Tablets 5.22.1 Release Notes
date: 2019-04-16
description: Maintenance release with Java 11 Docker support, Smart Rules output parameter improvements,
    7 bug fixes, and library updates.
---

OpenL Tablets **5.22.1** is a maintenance release containing improvements, bug fixes, and library updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

* Output parameter is populated from an input field in case the input is `Object` in Smart Rules.
* Docker containers work on Java 11.

## Bug Fixes

* Fixed: TBasic does not work in WebSphere 8.5.
* Fixed: Hints to external conditions do not work if the parameter type is not specified.
* Fixed: Incorrect matching of columns with External Conditions.
* Fixed: Multi-row Data tables work incorrectly when empty values are defined.
* Fixed: The error "The element is null" is presented to the user if a condition could match ambiguously.
* Fixed: `NullPointerException` in logs when ASM v4.2 is located in the classpath.
* Fixed: A value from the previous non-empty cell is returned from a Data table if the datatype second level of nesting
  is an array with a specified PK.

## Library Updates

| Library             | Version |
|:--------------------|:--------|
| POI                 | 4.1.0   |
| Commons Collections | 4.3     |
| Commons Codec       | 1.12    |
| Ehcache             | 2.10.5  |

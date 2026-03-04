---
title: OpenL Tablets 5.25.12 Release Notes
date: 2022-07-25
description: Introduced the Date() constructor and date manipulation functions, automatic index.lock removal in WebStudio,
    bug fixes in Rule Services, WebStudio, and the core engine, and library updates.
---

OpenL Tablets **5.25.12** is a maintenance release that introduces the `Date()` constructor and supplementary date
manipulation functions, adds automatic `index.lock` removal in WebStudio, resolves several bugs, and updates libraries.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## New Features

### Core

* Introduced the `Date()` function/constructor and supplementary functions for manipulations with the `Date` object.

## Improvements

### WebStudio

* The `index.lock` file is now automatically removed during Git repository initialization at startup.

## Bug Fixes

### Rule Services

* Fixed deployment deadlock issues.

### WebStudio

* Fixed error displays on merged table rows.
* Fixed incorrect validation messaging for the Time Format field.
* Fixed pop-up closure issues when using the browser back button.
* Fixed incorrect module display in breadcrumb navigation.
* Eliminated system virtual tables from search results.
* Fixed UI issues occurring when the Design repository is unavailable.

### Core

* Fixed the `contains(StringRange, String)` function to correctly handle ranges beginning at zero.

## Library Updates

| Library          | Version          |
|:-----------------|:-----------------|
| Spring Framework | 5.3.22           |
| Spring Security  | 5.7.2            |
| Log4j            | 2.18.0           |
| Tomcat           | 9.0.64           |
| Jetty            | 9.4.48.v20220622 |
| Netty            | 4.1.79.Final     |
| Groovy           | 3.0.11           |
| Bouncy Castle    | 1.71             |
| Amazon S3 SDK    | 1.12.262         |
| OWASP            | 7.1.1            |

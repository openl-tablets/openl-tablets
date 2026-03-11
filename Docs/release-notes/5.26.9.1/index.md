---
title: OpenL Tablets 5.26.9.1 Release Notes
date: 2023-06-08
description: Dynamic references in table properties, fixed slow test table opening,
    Maven plugin classifier handling, and AWS SSE header fix.
---

OpenL Tablets **5.26.9.1** is a hotfix release adding dynamic references in table properties and fixing several issues with test table performance, Maven plugin, and AWS SSE headers.

## Improvements

### Core

* An ability to use the dynamic references in the properties is added.

## Bug Fixes

### WebStudio

* Fixed test tables taking a very long time to open.

### Maven Plugin

* Fixed OpenL Maven plugin failing if a provided dependency has a classifier.

### Repository

* Fixed AWS SSE header for `.modification` file.

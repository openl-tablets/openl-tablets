---
title: OpenL Tablets 5.26.17 Release Notes
date: 2024-04-30
description: Fixed decision table indexing for contains() function, Azure BLOB repository functionality,
    and library updates.
---

OpenL Tablets **5.26.17** is a maintenance release that fixes decision table indexing for the `contains()` function, resolves Azure BLOB repository issues, and updates several libraries.

## Bug Fixes

### Core

* Fixed index in decision tables not being created for conditions with the `contains(array, elem)` function.

### Repository

* Fixed the repository of `Azure-blob` type being non-functional.

## Library Updates

| Library                | Version                  |
|:-----------------------|:-------------------------|
| JGit                   | 6.9.0.202403050737-openl |
| Flyway                 | 4.2.0.3                  |
| Netty                  | 4.1.109.Final            |
| Azure Blob Storage SDK | 12.25.4                  |
| Amazon AWS SDK         | 2.25.38                  |

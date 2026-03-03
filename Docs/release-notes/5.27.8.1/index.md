---
title: OpenL Tablets 5.27.8.1 Release Notes
date: 2024-10-01
description: Patch release fixing two ClassCastException issues related to SpreadsheetResult
    and field naming.
---

OpenL Tablets **5.27.8.1** is a patch release resolving two `ClassCastException` issues reported as known issues in 5.27.8.

## Bug Fixes

* Fixed `ClassCastException` when copying an element that contained a field with the name `class`.
* Fixed `ClassCastException` when the cell type was a combination of a `SpreadsheetResult` array and a single `SpreadsheetResult` value.

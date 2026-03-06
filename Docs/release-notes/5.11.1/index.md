---
title: OpenL Tablets 5.11.1 Release Notes
date: 2013-12-02
description: Improves Open Table performance in the Rules Editor and fixes 6 bugs in rule dispatch, Excel export,
    method filter tags, repository tree memory leaks, and UI freezing.
---

OpenL Tablets **5.11.1** is a patch release with a performance improvement and bug fixes.

## Improvements

**WebStudio:**

* Performance enhancements for the Open Table feature in the Rules Editor.

## Bug Fixes

**Core:**

* Fixed: Issue with the rule dispatch mechanism when property values overlap.

**WebStudio:**

* Fixed: Export functionality for Excel files (`.xls`, `.xlsx`) via the Rules Editor.
* Fixed: Export process for spreadsheet files (`.xls`, `.xlsx`) via the Repository.
* Fixed: Unintended automatic insertion of empty `<method-filter>` tags during Rules Configuration editing.
* Fixed: Multiple memory leak issues in the Repository tree component.
* Fixed: UI freezing when errors are encountered.

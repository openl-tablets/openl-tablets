---
title: OpenL Tablets 5.22.4 Release Notes
date: 2019-07-18
description: Release adding Git branch support with branch patterns and project operation comments,
    memory optimization for large XLSX files, and bug fixes.
---

OpenL Tablets **5.22.4** includes improvements, bug fixes, and known issues.

## Improvements

**Core:**

* Memory usage for XLSX files containing a large number of empty cells with a non-default style is optimized.

**Git Repository:**

* Git branches are now supported.
* A field for a new branch pattern is added.
* A comment field for DELETE, ERASE, RESTORE, CREATE, and COPY project operations is added.
* Ability to specify a directory for saving a project in case of a non-flat structure repository is added.

## Bug Fixes

**WebStudio:**

* Fixed: A project containing many errors takes a very long time to open.

**Git Repository:**

* Fixed: WebStudio is sometimes unresponsive.
* Fixed: The "Changes check interval" setting is not applied.

## Known Issues

**Git:**

* Comment for the Copy Project action is not displayed on the revisions tab.
* No error is presented in the UI if the user switches to a removed branch in WebStudio.
* The "New branch pattern" field is not allowed to be empty.
* The new branch pattern feature does not work if the user enters a pattern containing braces with an unsupported
  placeholder.
* `JGitInternalException` is presented to the user if the repository has a non-flat folder structure and the user enters
  the folder name of an existing folder with different capitalization.
* No validation for the Path in the repository field.

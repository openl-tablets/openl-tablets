---
title: OpenL Tablets 5.21.14 Release Notes
date: 2019-04-25
description: Introduces Git repository support (beta) with branch management improvements
    and 12 bug fixes for repository operations, settings restoration, and concurrent user workflows.
---

OpenL Tablets **5.21.14** introduces Git repository support in beta and includes repository improvements and bug fixes.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## New Features

* Support of Git repository (beta).

## Improvements

**Repository:**

* Improved performance of branch switching.
* New branch is set as active after copying.
* Support for a pattern for the New Branch name.

## Bug Fixes

**Core:**

* Fixed: Merged condition with 2 parameters and `==` relation does not work.

**Repository:**

* Fixed: Button "Deploy" does not appear for saved Deploy Configuration.
* Fixed: No commit messages generated for "Create" and "Copy" actions.
* Fixed: "Restore default settings" button does not work for Design Repository.
* Fixed: Internal Server Error after restoring default settings.
* Fixed: Possible to apply incorrect repository settings.
* Fixed: Error in log/UI when changing local path and selecting a non-master branch.
* Fixed: Project editing is locked when multiple users edit the same project in different branches.
* Fixed: "Restore default settings" button functions identically to "Apply" with incorrect settings.
* Fixed: Forbidden folder names in the "New Branch Name" field cause an error.
* Fixed: Error appears on project creation if a project with the same name previously existed.
* Fixed: Incorrect version of `rules-deploy.xml` is displayed when opening a previous version.
* Fixed: WebStudio becomes unresponsive during simultaneous multi-user operations.

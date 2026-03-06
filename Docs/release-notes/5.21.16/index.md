---
title: OpenL Tablets 5.21.16 Release Notes
date: 2019-06-20
description: Patch release fixing 7 bugs in Repository, core SpreadsheetResult handling, and Docker container issues.
---

OpenL Tablets **5.21.16** is a patch release containing bug fixes.

## Bug Fixes

**Repository:**

* Fixed: Default message is not populated into commit message when a user uploads a project via REST API.
* Fixed: "Internal Server Error" is presented to the user on file compare.
* Fixed: WebStudio sometimes hangs when a Git repository is used.
* Fixed: Cannot delete a project that was copied with the "do not link to origin project" copy option.

**Core:**

* Fixed: A whole `SpreadsheetResult` is returned as the actual value instead of a single cell if the test result column
  contains a reference to a Data table.

**Docker:**

* Fixed: A web context is created twice in Docker containers.
* Fixed: Deployment of rules fails sometimes.

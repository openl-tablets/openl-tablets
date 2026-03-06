---
title: OpenL Tablets 5.11.2 Release Notes
date: 2014-02-06
description: Patch release fixing 3 bugs in web services JAR unpacking on VFS, memory leaks during hot redeployment,
    and stale projects in repository after directory changes.
---

OpenL Tablets **5.11.2** is a patch release containing bug fixes.

## Bug Fixes

**Web Services:**

* Fixed: Problem with unpacking JAR files on VFS.
* Fixed: Multiple memory leaks that occurred during frequent hot redeployment operations via WebStudio.

**WebStudio:**

* Fixed: Projects remain visible in the repository even after modifying the workspace, history, design, and production repository directories.

---
title: OpenL Tablets 5.20.1 Release Notes
date: 2017-10-30
description: Patch release updating DEMO to Tomcat 8.5.23 and fixing 2 bugs in the Maven plugin and project status detection.
---

OpenL Tablets **5.20.1** is a patch release containing an update and bug fixes.

## Improvements

* Updated DEMO with Tomcat 8.5.23.

## Bug Fixes

* Fixed: NPE in `openl-maven-plugin` during the `test` phase if the `error()` function is invoked.
* Fixed: Incorrect project status if the user workspace path contains a symbolic link.

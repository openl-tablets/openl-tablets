---
title: OpenL Tablets 5.23.10 Release Notes
date: 2020-12-24
description: Maintenance release fixing 9 bugs in WebStudio, core rules engine, and Rule Services,
    including Smart Lookup, branch name validation, and deployment issues, plus library updates.
---

OpenL Tablets **5.23.10** is a maintenance release containing bug fixes and library updates.

## Bug Fixes

**WebStudio:**

* Fixed: A non-informative error is presented to the user when a branch name contains spaces.
* Fixed: The validation set in "Branch name pattern" does not work.
* Fixed: Impossible to create a user with one symbol in the username.
* Fixed: Spreadsheets cell type is not identified properly in some cases.

**Core:**

* Fixed: Conditions are matched incorrectly for Smart Lookup.
* Fixed: The "Method is ambiguous" error occurs when multiple methods match input parameters but one is preferable.
* Fixed: A non-informative error message is presented to the user if a SmartRules table has the wrong structure.

**Rule Services:**

* Fixed: Deployment failure of deploy configuration when the first project contains the parent of a datatype in the
  second project.
* Fixed: Deployment failure of a service in non-lazy mode when an interceptor changes the return type.

## Library Updates

| Library          | Version               |
|:-----------------|:----------------------|
| Spring Framework | 5.2.12.RELEASE        |
| Spring Security  | 5.4.2                 |
| Jackson          | 2.11.4                |
| Tomcat           | 9.0.41                |
| Swagger UI       | 3.38.0                |
| JGit             | 5.10.0.202012080955-r |
| XStream          | 1.4.15                |

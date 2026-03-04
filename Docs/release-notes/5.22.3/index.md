---
title: OpenL Tablets 5.22.3 Release Notes
date: 2019-07-03
description: Patch release fixing 5 bugs in core data table parsing, Rule Services WADL generation and redeployment, and Docker container issues.
---

OpenL Tablets **5.22.3** is a patch release containing bug fixes.

## Bug Fixes

**Core:**

* Fixed: Arrays and complex datatype fields are parsed differently in Data tables.

**Rule Services:**

* Fixed: WADL is not generated if the webservice has a runtime context.
* Fixed: A webservice is not redeployed via a `PUT` request.

**Docker:**

* Fixed: A web context is created twice in Docker containers.
* Fixed: Deployment of rules fails sometimes.

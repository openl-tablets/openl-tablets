---
title: OpenL Tablets 5.21.12 Release Notes
date: 2018-12-19
description: Patch release fixing 3 bugs in Trace functionality, Test table index access,
    and array Transform operation null handling.
---

OpenL Tablets **5.21.12** is a patch release containing bug fixes.

## Bug Fixes

**WebStudio:**

* Fixed: Internal error in Trace functionality when refreshing the target table.

**Core:**

* Fixed: Impossible to get an element by index from a result in Test tables with a foreign key table.
* Fixed: `Transform` array index operation does not exclude nulls from the result.

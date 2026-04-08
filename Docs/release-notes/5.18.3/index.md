---
title: OpenL Tablets 5.18.3 Release Notes
date: 2016-11-11
description: Switches logging to slf4j/logback, refactors the Maven plugin with ant removal and interface generation,
    and fixes 2 bugs in type casting and URL handling on Linux.
---

OpenL Tablets **5.18.3** is a patch release with a logging improvement, Maven plugin refactoring, and bug fixes.

## Improvements

* Switched logging from Log4j to slf4j/Logback.

**Maven Plugin:**

* Removed dependency on the `ant` library.
* Added generation of service interface and beans for OpenL projects.
* Made beans generation optional.

## Bug Fixes

* Fixed: Type casting issues between `String` and `StringValue` types.
* Fixed: Web services functionality without a trailing slash in URLs on Linux.

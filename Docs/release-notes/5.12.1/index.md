---
title: OpenL Tablets 5.12.1 Release Notes
date: 2014-08-28
description: Adds custom project templates in WebStudio, SpreadsheetResult return type in web services,
    and fixes 3 bugs in service hang, project management, and Linux startup.
---

OpenL Tablets **5.12.1** is a patch release with improvements and bug fixes.

## Improvements

**WebStudio:**

* Users can now add custom project templates for creating new projects.

**Web Services:**

* Added support for returning `SpreadsheetResult` type data.
* Enhanced configuration options for rules repository settings.

## Bug Fixes

**Web Services:**

* Fixed: Web service hangs when projects contain 4 or more module dependencies.

**WebStudio:**

* Fixed: Inability to close, delete, or erase projects containing `properties-file-name-processor` in `rules.xml`.
* Fixed: Startup problems when running WebStudio under ROOT in Linux environments.

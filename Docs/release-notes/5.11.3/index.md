---
title: OpenL Tablets 5.11.3 Release Notes
date: 2014-03-28
description: Patch release fixing 6 bugs in WebStudio ZIP upload, Production repository display, Run table settings,
    module export, repository export, and out-of-memory errors in web services.
---

OpenL Tablets **5.11.3** is a patch release containing bug fixes.

## Bug Fixes

**WebStudio:**

* Fixed: When a ZIP file consists of 1 folder and files in the root, files from the root are not uploaded when creating
  a new project.
* Fixed: After refreshing, all deployed projects disappear from the Production repository.
* Fixed: Checkboxes from Run table settings work incorrectly in several cases.
* Fixed: "Export module" does not work via the Rules Editor.
* Fixed: "Export file" does not work via the Repository when rules are located in a folder.

**Web Services:**

* Fixed: `OutOfMemoryError` exception when handling several parallel rule requests.

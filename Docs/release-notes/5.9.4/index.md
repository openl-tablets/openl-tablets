---
title: OpenL Tablets 5.9.4 Release Notes
date: 2012-11-01
description: Introduces multi-user security, user management UI, multiple production repositories, project versioning
    with business dimension properties, new date functions, and 32 bug fixes.
---

OpenL Tablets **5.9.4** is a feature release introducing access control, multi-repository support, project versioning,
and significant WebStudio improvements.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## New Features

### Security Modes

Two security modes are now available:

* **Single-user** (default): No additional setup required.
* **Multi-user**: Supports access control via user rights. Requires external database configuration per the Installation
  Guide's "Configuring External User Database" section. Access control is based on users and user groups — all
  privileges are assigned to a group and granted to users after they are included in that group.

### User Management

UI-based user management added for Multi-user mode.

### Multiple Production Repositories

Users can now deploy projects to multiple production repositories via the WebStudio UI.

### Project Versioning

* Business dimension properties added to rule projects; modules, folders, and files inherit these properties.
* System properties added: `createdBy`, `createdOn`, `modifiedBy`, `modifiedOn`.
* Revision numbering changed to single digits.
* Users can comment new project revisions.

## Improvements

**Core:**

* New date methods: `dayOfWeek`, `dayOfMonth`, `dayOfYear`, `weekOfMonth`, `weekOfYear`, `second`, `minute`, `hour`,
  `hourOfDay`, `amPm`.
* New syntax: `> dataTable.requiredfield keyfield` for separate field testing.

**WebStudio:**

* Option to skip adding table system properties via Administration control.
* UI can now specify `custom.spreadsheet.type` and `dispatching.mode` properties.
* Button/status renames: "Check in/out" → "Save/Edit", "Checked out" → "Editing", "version" → "Revision", "Redeploy" → "
  Deploy".
* Check/uncheck all test cases in Run/Trace/Benchmark dialogs.
* Recently visited tables display with appropriate versions and properties.
* Project names auto-populated from uploaded ZIP files.
* Multi-module project creation via simultaneous Excel file uploads.
* Unified project creation under a single "Create project" button.
* Multiple UI usability improvements.

**Library Updates:**

* Oracle Mojarra upgraded to 2.1.11.

## Bug Fixes

**WebStudio:**

* Fixed: Explanation in the RESULT column not functioning.
* Fixed: `system-settings` folder created in the default location despite a custom `webstudio.home` setting.
* Fixed: Save button available when no changes have been made.
* Fixed: Module names with spaces create duplicate files after include operations.
* Fixed: Files with spaces display as `%` characters in the Copy and Create table wizards.
* Fixed: Old deployment project versions reset on refresh.
* Fixed: Missing error message when no file is selected for upload.
* Fixed: Unnecessary data types appearing in wizards.
* Fixed: Table styles broken after property addition.
* Fixed: Services `.xml` visible in the project tree.
* Fixed: Out-of-memory errors during WebStudio use.
* Fixed: Folders with `.xls`/`.xlsx` in names displayed as files.
* Fixed: Foreign table data not sorting.
* Fixed: Date values parsed incorrectly in foreign key tables.
* Fixed: Formula results incorrect after Rules Editor edit.
* Fixed: UI elements misaligned after browser resizing.
* Fixed: HTTP 404 error in "No Security" mode.
* Fixed: "Edit cell with error" icon non-functional.
* Fixed: All table types parsed as "Other".
* Fixed: Collections in Trace display with system information.
* Fixed: "Detailed trace" checkbox not hidden when no Decision Table is being traced.
* Fixed: Copy as new version fails for tables with errors.
* Fixed: Empty results when running all tests for `runmethod` tables.

**Core:**

* Fixed: Int range parsing error with the minimum integer value.
* Fixed: Spreadsheet parsing failures on some projects.
* Fixed: Date operation errors.
* Fixed: Field execution errors from data tables.
* Fixed: Positive/negative operator inconsistency — positive operator returns `null` instead of the value.
* Fixed: Occasional `UnsupportedOperationException` in `SpreadsheetResult`.

**Web Services:**

* Fixed: Performance issues with large rule JAR loading.
* Fixed: Architecture change for different service types.

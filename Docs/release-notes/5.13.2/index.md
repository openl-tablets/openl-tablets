---
title: OpenL Tablets 5.13.2 Release Notes
date: 2014-11-20
description: Adds file upload warnings and deploy configuration validation in WebStudio,
    and fixes 3 bugs in WSDL naming, test result display, and transitive dependency validation.
---

OpenL Tablets **5.13.2** is a patch release with improvements and bug fixes.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## Improvements

**WebStudio:**

* Added a warning when uploaded files have different names than the originals.
* Implemented service name and service class validation in the Rules Deploy Configuration dialog.

## Bug Fixes

**Web Services:**

* Fixed: `<wsdl:service name>` and `<wsdl:port binding>` used "Virtual Module" instead of the actual project name.

**WebStudio:**

* Fixed: Display issues for Rules table test results with multiple conditions.
* Fixed: Validation error for modules in projects with multiple transitive dependencies.

---
title: OpenL Tablets 5.24.1 Release Notes
date: 2021-04-01
description: Consolidated Rules deploy configuration template fields, fixed 20 bugs across WebStudio, repository,
    and the core engine, and updated libraries including Spring Framework, Apache CXF, and Swagger UI.
---

OpenL Tablets **5.24.1** is a maintenance release that consolidates Rules deploy configuration template fields, resolves
20 bugs across WebStudio, repository, and the core engine, and updates libraries.

## Improvements

### WebStudio

* The "Intercepting template class" and "Annotation template class" fields have been consolidated into a single *
  *Template class** field in the Rules deploy configuration.

## Bug Fixes

### WebStudio

* Fixed the module list on the left panel not updating in the Editor after module duplication.
* Fixed a `NullPointerException` occurring when resolving multiple duplicate data tables (3 or more instances).
* Fixed Demo WebStudio failing to start after adding a second repository and removing the first.
* Fixed export functionality breaking for modules outside the project root folder.
* Fixed project caching errors appearing in logs for Git repositories with non-flat structures.
* Fixed array declaration links not being displayed.
* Fixed table column deletion triggering errors.
* Fixed records not appearing in Local Changes after module regeneration via OpenAPI import.
* Fixed the color selection control overlapping input parameters on the Trace screen.
* Fixed column names duplicating to adjacent cells when adding rows to tables with two or more columns.

### WebStudio Repository

* Fixed a Revisions tab error when a "Copy project" template lacks the `project-name` placeholder.
* Fixed branch creation failing with incorrect branch pattern validation.
* Fixed a Repository tab error in Demo WebStudio on Linux systems.
* Fixed the Unlock project action displaying an incorrect pop-up style with no success message.
* Fixed empty deploy configuration deployment not providing an error notification.
* Fixed a non-descriptive error message when project or configuration names match system reserved words.

### Core

* Fixed a spreadsheet table not being able to return the `void` type.
* Fixed `StringRange` not supporting spaces.
* Fixed smart rules returning values despite a `void` return type declaration.
* Fixed the spreadsheet return cell hint displaying an incorrect type for wrong return type headers.

## Library Updates

| Library          | Version        |
|:-----------------|:---------------|
| Spring Framework | 5.2.13.RELEASE |
| Spring Security  | 5.4.5          |
| Apache CXF       | 3.4.3          |
| Swagger UI       | 3.41.1         |
| XStream          | 1.4.16         |

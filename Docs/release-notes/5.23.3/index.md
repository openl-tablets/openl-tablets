---
title: OpenL Tablets 5.23.3 Release Notes
date: 2020-06-02
description: Release with project revision tracking, runtime context properties in Datatype tables,CORS and
    OpenAPI support, textJoin/textSplit functions, circular datatype dependencies, 49 bug fixes, and library updates.
---

OpenL Tablets **5.23.3** includes new features, improvements, bug fixes, and library updates.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## New Features

* Project revision tracking in Deployment.
* Ability to define runtime context properties in Datatype tables, potentially eliminating `RuntimeContext` parameters
  in Rule Services.
* CORS support in Rule Services.
* OpenAPI v3 support in Rule Services (Swagger v2 is deprecated).

## Improvements

**Core:**

* Added `textJoin` and `textSplit` String operations.
* Added support for circular datatype dependencies, similar to Java.
* Compilation performance of Spreadsheet tables is improved.
* Added automatic determination of `SpreadsheetResult` cell types.
* Added the ability to define expressions for Constants.

**Rule Services:**

* Simplified the configuration for output models.
* Supported customization for the Jackson Object Mapper via external MixIn classes.

**WebStudio:**

* Displaying matching hints in WebStudio even if a SmartRules table has issues.
* The default value for "Unlimited number of copies" is changed from unlimited to 100.
* Speed up UI by removing the Created At / Created By fields from the UI.

**Repository:**

* Only the first 6 symbols of a revision number are displayed on the Project List page.
* Business Revision (author + date) is displayed instead of the hash revision number.

**Docker:**

* Docker images are configured to utilize all accessible memory by default.

## Bug Fixes

**Demo:**

* Fixed: Project deployment failure on Demo on Java 8 and 13 in Windows 10.
* Fixed: Demo is not working if a white space is present in the path to the Demo folder.

**WebStudio:**

* Fixed: `NullPointerException` in `org.openl.rules.lang.xls.types.meta.BaseMetaInfoReader` appears in the log file.
* Fixed: Internal server error when a user changes `openl.home` if it was defined in a properties file.
* Fixed: No hints are displayed for a SmartRules table in case of an incorrect return compound datatype.
* Fixed: WebStudio fails to start if a placeholder with an invalid variable is set in properties.
* Fixed: `NullPointerException` in the log on opening a test table.
* Fixed: A very long scrollbar in case of test failure.
* Fixed: An error when editing Module info multiple times.
* Fixed: The "Restore Defaults" button does not immediately apply default settings.
* Fixed: "Run into file" does not work if some cells have the datatype `SpreadsheetResult[]`.
* Fixed: Repository UI is broken if the user entered an invalid time or date format.
* Fixed: The error "Sorry! Something went wrong" (`java.lang.StackOverflowError`) is presented to the user.
* Fixed: WebStudio crashes on entering an invalid Workspace Directory name.
* Fixed: Project failed to open with the `bruteForceHeaders` error.
* Fixed: Full revision number is not presented to the user on clicking the short number.
* Fixed: Short revision number is underlined.
* Fixed: Warning messages are not shown in WebStudio in Data, Test, Datatype, and other tables.
* Fixed: Different style of validation messages appears on the same page across all Admin tabs.

**Repository:**

* Fixed: The message "This project revision is already deployed" is displayed for Repository2.
* Fixed: Deployment and Design repositories cannot be defined as Git remote at the same time.
* Fixed: Build failure in Git tests due to concurrent modification of `gc.log.lock` file.
* Fixed: Local Git repository crashes if WebStudio was stopped incorrectly.
* Fixed: The Datatype table creation wizard has Custom `SpreadsheetResult` types in the dropdown.
* Fixed: Slow performance in a Git-based repository on the Deploy action.
* Fixed: A lot of memory is consumed when working with Git.
* Fixed: Git Merge has issues with protected branches.
* Fixed: Slow performance in the Repository tab for projects with a large number of revisions.
* Fixed: Slow performance of large Git repositories in WebStudio.
* Fixed: The "Receive their updates" and "Send your updates" buttons are enabled after the user merged.
* Fixed: WebStudio installation failure if branches with the same name but different capitalization exist.
* Fixed: Project status is changed to "Closed" instead of "Deleted" if the message pattern was customized.
* Fixed: `ConcurrentModificationException` on parallel project creation.

**Core:**

* Fixed: `java.lang.IndexOutOfBoundsException` appears on calling a non-existing element from a list.
* Fixed: Class is not specified in JSON when an Object array is returned but a datatype for the array is defined.
* Fixed: Argument type mismatch error because of incorrect cast from element to array.
* Fixed: `NullPointerException` appears on opening a module if a datatype contains a reference to another datatype.
* Fixed: Range syntax like `.1-.2` does not work.

**Rule Services:**

* Fixed: `NullPointerException` on array index operation with an empty object.
* Fixed: Project deployment fails if an input datatype has parameters with the same name but different capitalization.
* Fixed: The list of "Resolver error" is presented to the user in Swagger.
* Fixed: Rule Services application does not work under Java 12+.
* Fixed: Rule Services application crashes if a `CustomSpreadsheetResult` data table is used in rules.
* Fixed: Asynchronous requests failure.
* Fixed: Rules compilation fails if a Spreadsheet stored in another module calls a Spreadsheet.
* Fixed: A project containing different tables with the same `id` property can be successfully deployed.
* Fixed: Lazy functionality does not work properly if ehcache is configured without data serialization.

**OpenL Maven Plugin:**

* Fixed: `StackOverflowError` appears in the `toString()` method during project building.
* Fixed: Parallel builds failure.

## Library Updates

| Library                   | Version              |
|:--------------------------|:---------------------|
| Spring Framework          | 5.2.6.RELEASE        |
| Spring Security           | 5.3.2.RELEASE        |
| Jackson                   | 2.10.4               |
| CXF                       | 3.3.6                |
| Log4j                     | 2.13.3               |
| JGit                      | 5.7.0.202003110725-r |
| Elasticsearch             | 6.8.8                |
| Spring Data Elasticsearch | 3.2.6.RELEASE        |
| DataStax Java Driver      | 4.5.1                |
| ASM                       | 8.0.1                |
| AWS SDK                   | 1.11.785             |
| XStream                   | 1.4.12               |
| Swagger UI                | 3.25.3               |
| Joda-Time                 | 2.10.6               |
| HikariCP                  | 3.4.5                |
| Commons Lang              | 3.10                 |
| Commons Codec             | 1.14                 |
| SnakeYAML                 | 1.26                 |
| dom4j                     | 2.1.3                |
| swagger-jaxrs             | 1.6.1                |

---
title: OpenL Tablets 5.23.9 Release Notes
date: 2020-12-14
description: Release with new transient datatype field markers, Smart Lookup and Smart Rules improvements,
    Pacific country support, 54 bug fixes across WebStudio, core engine, Rule Services, and Rule Repository.
---

OpenL Tablets **5.23.9** includes new features, improvements, and a significant number of bug fixes.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## New Features

* Added the ability to mark a field name suffix in datatype tables with a transient (`~`) or non-transient (`*`) marker.
  Transient fields are ignored when referencing a field and are not included in OpenAPI/Swagger/WSDL schemas.

## Improvements

**Core:**

* A test table is matched with an overloaded rule table if it has the same number of parameters.
* Support for Pacific countries is added: Fiji, American Samoa, Cook Islands, Papua New Guinea, Samoa, Solomon Islands,
  Tonga, and Vanuatu.
* The `TRUE` condition is now supported in Smart Lookups.
* The `Rule` column is now supported in Smart Rules.

**WebStudio:**

* User management: national and non-restricted symbols are supported in usernames.

**Rule Repository:**

* `Class.forName()` vulnerabilities are removed from repository configuration.
* All non-restricted symbols are supported in branch names in the Git repository.
* Placeholders `{0}`, `{1}`, `{2}` are replaced with `{project-name}` and `{username}`.
* "Invalid login attempts" settings are configurable in WebStudio.

## Bug Fixes

**WebStudio:**

* Fixed: UI is broken if the password on the remote Git server is changed and a user opened the branch management UI.
* Fixed: A spreadsheet using `JSONUtils` does not work upon refresh.
* Fixed: The list of enumerable properties is not sorted alphabetically.
* Fixed: A technical revision is displayed in the Save Changes dialog instead of a business revision.
* Fixed: Illegal symbols `"` and `|` are allowed for folders and projects.
* Fixed: Runtime context does not work for the Run JSON feature.
* Fixed: The pencil icon incorrectly navigates a user to the erroneous cell.
* Fixed: JBoss: Templates appear in the wrong order in Create Project from Template.
* Fixed: JBoss: UI is broken after clicking the Send My Updates button.
* Fixed: The error "Sorry! Something went wrong" is displayed on refreshing and clicking errors.
* Fixed: Run into File fails if the input parameter is a 2D array.
* Fixed: Two directories are created during installation if a user defines a working directory other than the default
  one.
* Fixed: A warning message is displayed on copying the project in the "In Editing" status into a separate project.
* Fixed: WebStudio consumes memory without any user activity.
* Fixed: A non-informative error message is displayed if a datatype in the project does not match the generated
  datatype.
* Fixed: WebSphere: An error is displayed on opening the previous revision of a project.
* Fixed: WebSphere: An error is displayed on clicking the Rules Deploy Configuration tab of a closed project.
* Fixed: Wrong history is displayed for the "Filter files by extensions" field.
* Fixed: Empty projects are created on the AWS server during parallel project creation.
* Fixed: An error is displayed if a user deletes the last column of the Run table.
* Fixed: A Visual Basic script error is displayed on project opening via WebStudio if a table is located on a hidden
  sheet.
* Fixed: The default value `100` for "The maximum count of saved changes for each project" cannot be set after "
  Unlimited number of copies" was once saved.
* Fixed: Tests for tables that contain errors can be run by a user.
* Fixed: Module selection does not work in the Create Test Wizard and module copying UI.
* Fixed: Intermediate calculation results are displayed incorrectly in Trace: instead of intermediate values, final
  values are displayed.
* Fixed: An incorrect error text is displayed for the wrong project dependency.
* Fixed: An error is displayed for a project with the only `*` dependency.
* Fixed: A wrong error message is displayed for circular dependencies.
* Fixed: An error is displayed on clicking a circular error and then refreshing.
* Fixed: A list of projects is not loaded in the Auto Deploy screen.

**Core:**

* Fixed: Expression result type is not identified properly due to an array index operation.
* Fixed: Build fails when an external `application.properties` is defined in the user's home directory.
* Fixed: In Smart Rules, input parameter fields are set to the wrong return object fields.
* Fixed: Enormous binding errors are displayed in WebStudio.
* Fixed: A wrong array cast happens in ternary operations.
* Fixed: An incorrect error message is displayed in the UI if the test table name is the same as the spreadsheet name.
* Fixed: The symbols `(`, `)`, and `+` are not supported in the filename pattern.
* Fixed: `NullPointerException` is presented in the returned result if no condition matched and one of the conditions is
  a Date condition.
* Fixed: The return type of the `flatten()` operation is incorrect.
* Fixed: The incorrect logic of error identification is applied for Rules tables.
* Fixed: The "Method Ambiguous" error is missing for MultiCall functionality.
* Fixed: The "Method not found" error is displayed if an alias datatype and array of custom datatype arguments are
  passed with a non-array argument, but the method expected an array argument.
* Fixed: `Collect` does not work for merged conditions in Smart Rules.
* Fixed: Spreadsheet name in a hint is displayed incorrectly if the cell exists in several spreadsheets with different
  names.
* Fixed: An error is logged if a table contains formulas starting with `_xlfn`.
* Fixed: A warning message "Ambiguous matching" appears if the table title is merged with the empty column.
* Fixed: `NullSafety` does not work in the `eq()` operation (`==` or `!=`).

**Rule Services:**

* Fixed: The `String` type is generated in OpenAPI for the `List` return type.
* Fixed: The error "Could not resolve reference: undefined undefined" is displayed in Swagger if the
  `AnySpreadsheetResult` schema contains a circular reference.
* Fixed: An incorrect field name is generated if the property name has a setter/getter in the JAR library.
* Fixed: Performance degradation is detected.

**Rule Repository:**

* Fixed: WebStudio throws an internal server error and fails to restart if the Design repository is connected to a clean
  remote Git repository.
* Fixed: A Git conflict is not resolved if the file is deleted.
* Fixed: The branch currently edited by a user is not protected from synchronization from another branch or user.

## Library Updates

| Library          | Version          |
|:-----------------|:-----------------|
| Spring Framework | 5.2.11.RELEASE   |
| Tomcat           | 9.0.40           |
| Jetty            | 9.4.35.v20201120 |
| Swagger UI       | 3.37.0           |
| CXF              | 3.3.8            |
| XStream          | 1.4.14           |
| Joda-Time        | 2.10.8           |

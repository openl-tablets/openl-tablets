---
title: OpenL Tablets 5.23.12 Release Notes
date: 2021-03-25
description: Maintenance release fixing 13 bugs in WebStudio, core rules engine, and Rule Services,
    including Smart Lookup, test tables, and spreadsheet cell reference issues.
---

OpenL Tablets **5.23.12** is a maintenance release containing bug fixes.

## Bug Fixes

**WebStudio:**

* Fixed: Repeated warnings appear in the log if all Excel files were deleted from the project with `rules.xml`.
* Fixed: The error "Something went wrong" is presented to the user on clicking a "Dependency is not found" error.
* Fixed: A duplicate record in a different register is created in the WebStudio users table for the same external user.
* Fixed: The error "Sorry! Something went wrong" is displayed if a user deletes a table when the project is opened in
  Excel at the same time.

**Core:**

* Fixed: Incorrect matching of horizontal conditions in Smart Lookup.
* Fixed: The `Date` function throws an `IllegalArgumentException` error at runtime if input arguments are invalid.
* Fixed: `NullPointerException` is presented in a test table if the output datatype of the rules table is not found.
* Fixed: `ArrayIndexOutOfBoundsException` in a rules table if the field datatype is not found in the Return column.
* Fixed: `NullPointerException` is presented in a rules table if a type in a condition is not found.
* Fixed: A Smart Lookup table of a certain structure is wrongly compiled as transposed.
* Fixed: The min/max feature does not work properly with a fully qualified class name in smart rules.
* Fixed: `NullPointerException` is presented in smart rules table if the return datatype array does not exist.
* Fixed: Converting `Double=null` to the `double` datatype returns an empty result instead of `0`.

**Rule Services:**

* Fixed: Deploying a project fails if there is a reference to a spreadsheet cell from another module.

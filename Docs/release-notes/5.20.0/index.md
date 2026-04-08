---
title: OpenL Tablets 5.20.0 Release Notes
date: 2017-10-26
description: Introduces SmartLookup and SmartRules tables, Active Directory and SSO integration, Amazon S3 storage,
    generic method support, and 29 bug fixes. Removes implicit Double to Integer casting.
---

OpenL Tablets **5.20.0** is a feature release introducing new table types, enterprise authentication support, and cloud
storage integration. This release also includes a breaking change removing implicit casting from `Double` to `Integer`.

## New Features

### SmartLookup and SmartRules Table Types

New table types allow writing inputs that are not conditions, with fuzzy search and compound return types supporting
Collections and Lists.

### Generic Method Support

Support for Generics in methods — specifically `GenericArrayType` and `TypeVariable`.

### Active Directory Integration

WebStudio can now be configured to use Active Directory. Authentication is possible using Active Directory credentials.

### Single Sign-On Support

Single Sign-On with CAS and SAML protocols is now supported in WebStudio.

### Enhanced Run Table Parameters

The Run Table page now shows appropriate input elements for input parameters, with type-specific controls.

### Maven Plugin Deploy Goal

Configuration support for the Maven plugin `deploy` goal has been added.

## Improvements

**Core:**

* Parallel execution for array parameter calls.
* Compound results without mandatory merges.
* Return type fixes for functions such as `flatten`.
* Compilation error for undefined TBasic variables.
* New functions: `concatenate()`, `slice()` for object arrays, `length()`, and `trim()` for strings.

**WebStudio:**

* Deployment confirmation messaging refined.
* Active Directory configuration in the Install Wizard.
* Production Repository renamed to Deployment Repository.
* Admin privileges for sorting groups/users.
* Map and list field population enabled.

**Repository & Web Services:**

* Amazon S3 storage integration.
* REST client privilege verification.
* Auto-generated `application.properties`.
* Method exposure with custom signatures.
* Empty project deployments omit SOAP/REST services.

**Performance:**

* Repository refactored to avoid loading `rules.xml` from all projects.
* Reduced repository scanning requirements.

## Bug Fixes

* Fixed: Incorrect operation of error links for merged cells.
* Fixed: Incorrect `SpreadsheetResult` returned when a Spreadsheet column or row name contains spaces.
* Fixed: No validation of already existing names for Datatype and Datatype Alias tables.
* Fixed: Error when the name of a method and the module name are the same.
* Fixed: No validation for the "Use the Range" field — same IDs, `-` in the `_id_` column.
* Fixed: Defined `ulp` for `BigDecimal` type values as half of the maximal `ulp` of both numbers.
* Fixed: Incorrect operation of `anyTrue()` function when the array is empty.
* Fixed: Incorrect checking of Alias datatype in input arguments at runtime.
* Fixed: Incorrect operation of `dateToString(date, dateFormat)` function when the `dateFormat` parameter contains a `$`
  sign.
* Fixed: Incorrect operation of `MethodSearch` between primitives and `Object`.
* Fixed: Eliminated redundant compile exceptions.
* Fixed: No ability to display elements of `HashSet`, `HashMap`, and other collections.
* Fixed: Incorrect behavior of input parameters when rerunning the same test case multiple times.
* Fixed: Issue with the compare functionality of an incorrect project.
* Fixed: Issue with "Condition is always true" warning.
* Fixed: Issue with Deploy Configuration refresh.
* Fixed: No ability to copy REST modules with path patterns when 2 or more modules with a pattern share the same name.
* Fixed: No ability to create a project with broken `rules.xml`.
* Fixed: Incorrect display of "Found duplicate of field" error.
* Fixed: Issue with adding a property to a table with two columns.
* Fixed: Incorrect behavior of comparison with "Empty".
* Fixed: Incorrect operation of versioning for JCR repository.
* Fixed: Issue with clicking the "Pencil" icon in the error message.
* Fixed: Incorrect project status for different users.
* Fixed: Incorrect error text when the first user deletes a deployment configuration and the second user unlocks it
  simultaneously.
* Fixed: Incorrect behavior during module updating.
* Fixed: After a project is unlocked, its status changes to "Closed" for all users.
* Fixed: Issue with creating a project through the workspace while a file is open.
* Fixed: Minor UI issues.

## Breaking Changes

### Implicit Double to Integer Casting Removed

The implicit casting from `Double` to `Integer` has been removed as it was error-prone. Update any rules that rely on
this implicit conversion.

## Library Updates

| Library | Version |
|:--------|:--------|
| POI     | 3.17    |
| CXF     | 3.1.13  |
| Jackson | 2.8.9   |

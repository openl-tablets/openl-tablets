---
title: OpenL Tablets 5.19.1 Release Notes
date: 2017-04-29
description: Introduces database storage for rules projects and deployments, array call support for multiple arrays,
    Maven plugin package goal, Auto Type Discovery enabled by default, and 6 bug fixes.
---

OpenL Tablets **5.19.1** is a feature release introducing database-backed repository storage, array call enhancements,
and Maven plugin improvements. Eclipse-based rules project support has been removed.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Breaking Changes](#breaking-changes)

## New Features

### Database Storage for Rules Projects and Deployments

A new database repository implementation is available:

* Uses a database via a simple JDBC connection.
* Reduces library dependencies.
* Simplifies the database schema.
* Enables simpler integration for embedded rules.

### Array Call for Multiple Arrays

Rules can now be separately calculated for each array element:

* Results in an array of the return type.
* Iteration order is unspecified and may change.
* Recommended when no interdependency exists between elements.

## Improvements

**Core:**

* Updated Spreadsheet tables for automatic cell type determination.
* Spreadsheet tables now honor dependencies across tables.
* `flatten()`, `add()`, and `addAll()` functions return specific type arrays.
* `import` keyword allows importing Java libraries or classes.
* Added `anyFalse()` and `allFalse()` functions.
* Improved `SimpleProjectEngineFactory` class.
* Supported `*` in `rules.xml` for classpath elements.

**WebStudio:**

* Improved Active Directory support for user management.
* Added hints for Spreadsheet cells showing type information.
* Added automatic adjustment of `rules-deploy.xml`.
* Improved ambiguous dispatch error messages.

**Rule Service:**

* Added overriding of placeholder properties.
* Single `application.properties` file configuration.
* Removed JCR-related library dependencies.
* New database storage preconfigured by default.

**Demo Package:**

* Added `application.properties` file.
* Moved `openl-demo` folder and `start` scripts to the root.
* Configured new database storage for the deployment repository.

**Maven Plugin:**

* Added `package` goal for creating a zipped artifact.
* Updated `generate` goal for variations and `RuntimeContext`.
* Added Maven build lifecycle support.

**Other:**

* Auto Type Discovery is now switched on by default.
* `slf4j-log4j` and `jcl-over-slf4j` moved to runtime scope.
* Updated CXF to 3.0.11.
* Upgraded Ehcache to 2.7.8.
* Removed `ant` and `jersey-common` library dependencies.

## Bug Fixes

* Fixed: Ternary `if` operator to use the second expression when the first is `null`.
* Fixed: SOAP request storage in Cassandra.
* Fixed: Incorrect URL generation for web services on Linux.
* Fixed: Drop-down lists for alias datatypes in the Editor.
* Fixed: FailOnMiss display issue.
* Fixed: Merged cells display in environment tables.

## Breaking Changes

### Eclipse-Based Rules Project Support Removed

Eclipse-based rules project support has been removed. Add a `rules.xml` file to any deprecated Eclipse-based projects.

### Auto Type Discovery Enabled by Default

`autoType` is now enabled by default. Review and check `autoType` property settings in existing projects.

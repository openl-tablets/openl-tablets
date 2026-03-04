---
title: OpenL Tablets 5.9.1 Release Notes
date: 2012-04-06
description: Introduces Web Services test framework, memory-based caching, customized multimodule dispatching,
    and 12 bug fixes. Updates Spring 3.1.1, Hibernate 4.1, CXF 2.5.1, and other libraries.
---

OpenL Tablets **5.9.1** is a feature release with new web service testing capabilities, caching improvements, and
library upgrades.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## New Features

### Test Framework for Web Services

Added the ability to include test projects based on OpenL Web Services. Two test types are supported: tests for rules
logic and tests for full rules logic including transport and binding.

### Cache in Web Services by Memory Footprint Size

New settings for caching in Web Services allow specifying the memory size to use.

### Multimodule with Customized Dispatching

An additional multi-module mode enables custom dispatching logic through user-defined classes rather than default
handling. Setup steps:

1. Create a Java interface representing the rules.
2. For each interface method, determine dispatching:
    * **Data tables**: Implement `ModuleDispatcherForData` and annotate with `@DispatchedData`.
    * **Rules**: Implement `ModuleDispatcherForMethods` and annotate with `@DispatchedMethod`.
3. Create a custom `RuleServiceInstantiationStrategyFactory` implementation returning
   `DispatchedMultiModuleInstantiationStrategy` and register it in `openl-ruleservice-override-beans.xml`.

## Improvements

**Core:**

* Generated attributes in Datatype classes are now sorted for enhanced version control compatibility.

**WebStudio:**

* Warning message when reverting changes with open Excel files.
* "By Category" view is now the default view.
* Tree arguments for expected/actual test results.
* Last viewed page displays instead of the Welcome page after refresh.
* Worksheet alphabetical sorting in the table creation wizard.

## Bug Fixes

**WebStudio:**

* Fixed: Cancel button in "Edit as new version"/"Copy" restores the original table.
* Fixed: Warning about changes in Excel file displays even before the user makes any changes.
* Fixed: Incorrect return values from overloaded property table nodes.
* Fixed: HTTP 500 error on trace for some nodes.
* Fixed: Array datatype alias value error not displayed.
* Fixed: Boolean defaults via `yes`/`no`/`y`/`n` now functional.
* Fixed: Datatype default values with spaces handling.
* Fixed: Phrase search in Chrome and IE 9.0.
* Fixed: Special character recognition in simple search.
* Fixed: After clicking the Cancel button, the "Project was modified" warning no longer appears.

**Core:**

* Fixed: Runtime `equals` method for generated datatypes in bytecode.
* Fixed: TBasic with void return type contains a return value in trace.

## Library Updates

| Library          | Version |
|:-----------------|:--------|
| Spring Framework | 3.1.1   |
| Spring Security  | 3.1.0   |
| EhCache          | 2.5.1   |
| Hibernate ORM    | 4.1     |
| Mojarra          | 2.1.7   |
| Javassist        | 3.15.0  |
| HSQLDB           | 2.2.8   |
| CXF              | 2.5.1   |

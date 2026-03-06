---
title: OpenL Tablets 5.19.6 Release Notes
date: 2017-09-13
description: Performance improvements to OpenMethodDispatcher and RuleService redeployment, reduced memory footprint,
    and 6 bug fixes for REST upload, datatype generation, and formula handling.
---

OpenL Tablets **5.19.6** is a patch release with performance improvements and bug fixes. The deprecated `IVocabulary`
class has been removed.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## Improvements

**Core:**

* Enhanced performance of `OpenMethodDispatcher.invokeInner()`.
* Addressed thread blocking concerns.
* Decreased memory footprint by eliminating `EqualsBuilder` and `HashCodeBuilder`.
* Optimized `RuleService.redeploy()` by avoiding duplicate `createService()` calls.

## Bug Fixes

* Fixed: Uploading locked projects through the REST interface.
* Fixed: Improper Java file generation from OpenL datatypes.
* Fixed: Null-safety issues in Spreadsheets Java generation.
* Fixed: Benchmark execution issues.
* Fixed: Trailing whitespace handling in keywords.
* Fixed: Formula behavior within conditions.

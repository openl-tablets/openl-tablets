---
title: OpenL Tablets 5.0.5 Release Notes
date: 2008-02-15
description: Introduces a JCR-based Repository, an AJAX-based Table Editor, Vocabulary support,
    the Business Expression Language (BEX), and Java Proxy wrapping for OpenL modules.
    Fixes multi-threaded wrapper issues and adds long arithmetic operators.
---

OpenL Tablets **5.0.5** is a major feature release introducing the repository, a new table editor, and the Business
Expression Language.

## New Features

### JCR-Based Repository

A content repository based on the Java Content Repository (JCR) standard is now available for storing and versioning
OpenL rules projects.

### AJAX-Based Table Editor

A new browser-based table editor uses AJAX for interactive cell editing of Excel tables directly in WebStudio.

### Vocabulary

Vocabulary support is introduced, enabling business-friendly naming and language customization for rules and properties.

### Business Expression Language (BEX)

The Business Expression Language (BEX) provides a more natural, Excel-like syntax for writing rule expressions in OpenL
Tablets.

### Java Proxy for OpenL Module Wrapping

OpenL modules can now be wrapped using a Java Proxy, providing a clean interface for invoking rules from Java code.

## Bug Fixes

* Fixed: `OpenLWrapper` used in a multi-threaded environment — a local copy of `RuntimeEnv` is now created per thread.
* Fixed: Added operators to handle `long` arithmetic correctly.

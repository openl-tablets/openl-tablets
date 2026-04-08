---
title: OpenL Tablets 5.20.2 Release Notes
date: 2017-11-20
description: Adds deprecated method warnings, pluggable Cassandra/Elasticsearch logging modules, 6 bug fixes,
    and updates 15 libraries including Spring Framework, Jackson, and CXF.
---

OpenL Tablets **5.20.2** is a patch release with improvements, bug fixes, and library updates.

## Improvements

**Core:**

* Added warning for `@Deprecated` methods.

**Web Services:**

* Made Cassandra/Elasticsearch logging store modules pluggable.

## Bug Fixes

* Fixed: Copy project dialog issue.
* Fixed: Casting from `Integer` to `Double`.
* Fixed: Casting from `Integer` to `Object`.
* Fixed: Overload table binding issue.
* Fixed: Incorrect calculation in Spreadsheet table with Auto Type Discovery enabled.
* Fixed: Incorrect conversion of national symbols in WebStudio.

## Library Updates

| Library            | Version        |
|:-------------------|:---------------|
| Spring Framework   | 4.3.12.RELEASE |
| Spring Security    | 4.2.3.RELEASE  |
| Jackson            | 2.9.2          |
| slf4j              | 1.7.25         |
| Logback            | 1.2.3          |
| CXF                | 3.1.14         |
| Commons Lang3      | 3.7            |
| Commons BeanUtils  | 1.9.3          |
| Commons Codec      | 1.11           |
| Commons IO         | 2.6            |
| HikariCP           | 2.4.13         |
| Jettison           | 1.3.8          |
| Cassandra Driver   | 3.0.7          |
| H2 DB              | 1.4.196        |
| MS SQL JDBC Driver | 6.2.2.jre7     |

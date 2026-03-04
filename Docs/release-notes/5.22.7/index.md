---
title: OpenL Tablets 5.22.7 Release Notes
date: 2019-12-11
description: Maintenance release with a default date format for REST services, two bug fixes,
    bitwise operations deprecation, and library updates.
---

OpenL Tablets **5.22.7** is a maintenance release containing an improvement, bug fixes, a deprecation, and library
updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Deprecations](#deprecations)
* [Library Updates](#library-updates)

## Improvements

* Ability to define a default date format for REST services.

## Bug Fixes

* Fixed: Deadlock happens on erasing a project when the Design repository is in Git.
* Fixed: Null-safety is not working when calling `getField((T[]) null)`.

## Deprecations

* Bitwise operations are deprecated.

## Library Updates

| Library       | Version          |
|:--------------|:-----------------|
| Jackson       | 2.10.1           |
| POI           | 4.1.1            |
| ASM           | 7.2              |
| cglib         | 3.3.0            |
| CXF           | 3.3.4            |
| Tomcat        | 9.0.27           |
| Jetty         | 9.4.24.v20191120 |
| Swagger UI    | 3.24.3           |
| swagger-jaxrs | 1.6.0            |
| SLF4J         | 1.7.29           |
| SnakeYAML     | 1.25             |

---
title: OpenL Tablets 5.24.3 Release Notes
date: 2021-06-24
description: Added Kubernetes health probes to Rule Services, improved WebStudio search scope and error display,
    fixed 14 bugs, and updated libraries including Spring, Tomcat, Jetty, and Apache CXF.
---

OpenL Tablets **5.24.3** is a maintenance release that introduces Kubernetes health probes in Rule Services, improves
WebStudio search scope and error stack trace display, resolves 14 bugs, and updates libraries.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## New Features

### Rule Services

* Implemented health probes for Kubernetes in Rule Services.

## Improvements

### WebStudio

* Added a **Scope** section to the Rules Editor search to allow searching either within the current module or the entire
  project.
* Added the ability to collapse error stack traces in the Rules Editor using an arrow button.

### Rule Services

* Extra methods can now be defined in OpenL Tablets annotation template services.

## Bug Fixes

### Core

* Fixed return values not being included in results when conditions do not match.
* Fixed deprecated `include` functionality for dependent modules.

### WebStudio

* Fixed a `java.lang.ArrayIndexOutOfBoundsException` error appearing in logs.
* Fixed empty folders being created after unlock for usernames with non-alphabetic symbols.
* Fixed empty directories being created in the user workspace for users with no open projects.
* Fixed the **Save** button not being enabled in the Resolve Conflicts window for branches with two conflicting
  projects.
* Fixed the "Add Repository" error occurring when the previous repository name ends with a long number.
* Fixed the search filter not supporting filtering by table type.
* Fixed the table type dropdown in the search filter not displaying correctly.
* Fixed usernames with non-alphabetic symbols not displaying correctly after a merge in revisions.
* Fixed tables not being copyable when using existing version numbers.
* Fixed table hints being truncated instead of displaying completely.

### Demo

* Fixed datatypes not being creatable in the demo application.

### Rule Services

* Fixed errors not being displayed in the response when a synchronous Cassandra save fails.

## Library Updates

| Library          | Version               |
|:-----------------|:----------------------|
| Spring Framework | 5.3.8                 |
| Spring Security  | 5.5.0                 |
| Apache CXF       | 3.4.4                 |
| Tomcat           | 9.0.48                |
| Jetty            | 9.4.42.v20210604      |
| Hibernate        | 5.4.32.Final          |
| JGit             | 5.12.0.202106070339-r |
| SnakeYAML        | 1.29                  |
| Commons Lang3    | 3.12.0                |
| Commons IO       | 2.10.0                |
| Swagger UI       | 3.50.0                |
| Bouncy Castle    | 1.69                  |

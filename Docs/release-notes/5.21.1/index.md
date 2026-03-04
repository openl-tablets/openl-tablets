---
title: OpenL Tablets 5.21.1 Release Notes
date: 2018-03-05
description: Adds Docker support for Rule Service, WebStudio, and DEMO, redesigns the Rule Service root page
    with updated Swagger UI, and fixes 9 bugs across Core, WebStudio, Rule Service, Repository, and Demo.
---

OpenL Tablets **5.21.1** is a patch release with improvements and bug fixes, including Docker support for all major
components.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## Improvements

**WebStudio:**

* Enabled creation of multiple repositories using the same Oracle DB instance with different usernames.
* Implemented validation for deployment configuration names.

**Rule Service:**

* Services that failed no longer redeploy automatically without updates.
* Redesigned root page with updated Swagger UI.
* Added Dockerfile support for Rule Service, WebStudio, and DEMO.

## Bug Fixes

**Core:**

* Fixed: Compilation failure in multi-threading scenarios using lazy compilation.
* Fixed: Incorrect array declaration in Data/Test tables.
* Fixed: Empty value assignment fails for arrays in Data/Test tables.

**WebStudio:**

* Fixed: Deleted projects remain in the Deployment Repository.

**Rule Service:**

* Fixed: REST services reject capitalized argument names.
* Fixed: Incorrect `basePath` in Swagger service description.

**Repository:**

* Fixed: DB repository deployment fails when using `org.apache.tomcat.jdbc.pool.DataSourceFactory`.

**Demo:**

* Fixed: Rule Service functionality fails under the security manager in JRE 9.

---
title: OpenL Tablets 5.22.5 Release Notes
date: 2019-07-24
description: Maintenance release adding a new webservice.war artifact with Cassandra logging
    and two bug fixes for JBoss EAP 7.2 and WebSphere compatibility.
---

OpenL Tablets **5.22.5** is a maintenance release containing an improvement and bug fixes.

## Improvements

**Rule Services:**

* A new `webservice.war` artifact with included Cassandra logging is added.

## Bug Fixes

**Rule Services:**

* Fixed: The `application-default.properties` file is not loaded on JBoss EAP 7.2.
* Fixed: Writing to Cassandra fails on WebSphere because of specific classloaders.

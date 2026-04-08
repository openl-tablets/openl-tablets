---
title: OpenL Tablets 5.25.8 Release Notes
date: 2022-02-18
description: Improved Rule Services with Swagger UI enhancements and multipart support, fixed 9 bugs in WebStudio and the core engine, and updated libraries.
---

OpenL Tablets **5.25.8** is a maintenance release that improves Rule Services with Swagger UI enhancements and multipart
annotation support, resolves 9 bugs in WebStudio and the core engine, and updates libraries.

## Improvements

### Rule Services

* Added references to all REST services on the Swagger UI page.
* OpenL Rules runtime errors are now logged.
* Added support for multipart annotations in Rule Services.
* Added customization of the `FAIL_ON_EMPTY_BEANS` Jackson property.

## Bug Fixes

### WebStudio

* Fixed a remote Git branch not being deleted in the local repository after being merged into the master branch via a
  merge request.
* Fixed the rules table name being modified in the "Available Tests/Runs" and "Target table" links.
* Fixed a link for a spreadsheet not being displayed when there are two spreadsheet results in the input.
* Fixed `StringIndexOutOfBoundsException` being displayed in the log file when opening a table of the `method` type.
* Fixed `NullPointerException` appearing in the log when one user deploys a project while another user has the
  Deployment tree open at the same time.
* Fixed `NullPointerException` "Something went wrong" being displayed to a user who selects "Search" in a spreadsheet
  cell menu.
* Fixed `fancytree.js` being loaded from `cdnjs.cloudflare.com` in trace pop-up windows.
* Fixed the latest merge commit not being displayed to a user when the merged branch contains no actual changes.

### Core

* Fixed the return column being matched by name incorrectly in a smart rules table.

## Library Updates

| Library          | Version          |
|:-----------------|:-----------------|
| Spring Framework | 5.3.16           |
| Spring Boot      | 2.6.3            |
| Tomcat           | 9.0.58           |
| Jetty            | 9.4.45.v20220203 |
| Netty            | 4.1.74.Final     |
| Slf4j            | 1.7.36           |
| SnakeYAML        | 1.30             |
| Swagger          | 1.6.5            |
| Swagger UI       | 4.5.0            |
| XStream          | 1.4.19           |
| JCodeModel       | 3.4.1            |
| Mockito          | 4.3.1            |

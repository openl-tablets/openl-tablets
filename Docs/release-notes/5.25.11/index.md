---
title: OpenL Tablets 5.25.11 Release Notes
date: 2022-05-26
description: Added unique request ID generation in Rule Services, published build information endpoint, 18 bug fixes across the core engine, WebStudio, and Rule Services, and library updates.
---

OpenL Tablets **5.25.11** is a maintenance release that introduces unique request ID generation in Rule Services,
publishes build information via a dedicated endpoint, resolves 18 bugs across the core engine, WebStudio, and Rule
Services, and updates libraries.

## Improvements

### Rule Services

* OpenL Rule Services now generates a unique identifier per each request and saves it to the Mapping Diagnostic
  Context (MDC) with the `requestID` key.
* Custom build information is published at the `admin/info/build.json` URL.

## Bug Fixes

### Core

* Fixed no check being performed on cast in index operations.
* Fixed an array not being definable in a spreadsheet table using the `$Step2:$Step1` operation when Step2 comes after
  Step1.
* Fixed columns with true conditions being incorrectly matched to an output object when the number of `IsTrue`
  conditions exceeds 8.
* Fixed rules compilation failing when `null` is used for calling methods with varargs.
* Fixed OpenAPI validation displaying an error for the `Object` type when a non-object result is generated.
* Fixed a compilation error occurring when the varargs parameter is omitted.

### WebStudio

* Fixed no link or hint being displayed for methods called in a rule with a merged condition.
* Fixed incorrect input data being displayed when running a table with several input parameters.
* Fixed a page with a script being accessible in WebStudio via direct links.
* Fixed `NullPointerException` being displayed in WebStudio Editor for a data table when an incorrect datatype is
  specified.
* Fixed UI corruption when a cell intended for a warning message contains the `$` symbol.
* Fixed a project not being addable to a deploy configuration when the project name contains `&`.
* Fixed an internal server error being displayed in WebStudio when an invalid URL is used.
* Fixed the `java.lang.StringIndexOutOfBoundsException` error being logged when opening a smart rules table.

### Rule Services

* Fixed a NullPointerException being logged when stopping Rule Services if no projects are deployed.
* Fixed multiple warning messages being saved to Rule Services logs on the AWS S3 server upon project deployment.
* Fixed the error response structure being different for Kafka and Rule Services calls.
* Fixed `PropertiesLoader` throwing multiple warning messages when some properties are invalid and logging to Cassandra
  is enabled.

## Library Updates

| Library          | Version          |
|:-----------------|:-----------------|
| Spring Framework | 5.3.20           |
| Spring Security  | 5.6.3            |
| Spring Boot      | 2.6.7            |
| Apache CXF       | 3.5.2            |
| Jackson Databind | 2.13.3           |
| Jetty            | 9.4.46.v20220331 |
| Netty            | 4.1.77.Final     |
| Swagger UI       | 4.10.3           |
| Kotlin Stdlib    | 1.6.21           |
| GSON             | 2.9.0            |
| esapi            | 2.4.0.0          |
| OWASP            | 7.1.0            |

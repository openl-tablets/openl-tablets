---
title: OpenL Tablets 5.26.4 Release Notes
date: 2023-01-12
description: Custom error message testing, locale context from Accept-Language header, localized Rule Services
    responses, DEMO improvements, extensive bug fixes, and library updates.
---

OpenL Tablets **5.26.4** is a maintenance release introducing custom error message testing, localized Rule Services responses, DEMO improvements, extensive bug fixes across all components, and library updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

### Core

* Custom error message testing capability via test tables.
* Enhanced memory usage in SmartRules parsing.
* Warnings for matching arguments with case-sensitive naming differences.

### Rule Services

* The `locale` context variable now retrieves from the `Accept-Language` HTTP header.
* Localized OpenL Tablets Rule Services responses based on browser settings.
* Customizable system error responses via `JAXRSExceptionMapper` override.
* Error arrays returned without `body` property wrapping.

### Demo

* Browser navigation enabled via double-clicking the start script.
* Tutorial 8 (Smart Rules/Smart Lookup Tables) project opened; Tutorial 4 closed.
* New OpenL Tablets DEMO version migration without manual folder copying.

## Bug Fixes

### Core

* Fixed `StackOverflow` error from return parameter calculations.
* Fixed slash syntax failures in simple rules.
* Fixed excessive error reporting for missing external projects/modules.
* Fixed extra errors from non-existing enum properties.
* Fixed missing constructor parameter validation.
* Fixed silent failure for non-existing method calls with matching aliases.
* Fixed spurious `'_res_' column missing` warnings in run tables.
* Fixed `StringIndexOutOfBoundsException`/`ArrayIndexOutOfBoundsException` with special symbols.
* Fixed incorrect primitive type casting selection.
* Fixed `StringRange` parsing issues with backslashes, periods, and spaces.

### WebStudio

* Fixed infinite loading after array comma insertion.
* Fixed internal server error with Level = "None" filter.
* Fixed `ClassCastException` on dependent project opening.
* Fixed module loading errors with duplicate "input" parameters.
* Fixed module loading failures from datatype date mismatches.
* Fixed SpreadsheetResult conversion errors when `custom.spreadsheet.type = false`.
* Fixed Git repository corruption with submodules.
* Fixed unresolved C2 condition parameter datatypes in merged conditions.
* Fixed local history loss after project copying.
* Fixed Postgres-related duplicate project tag display issues.
* Fixed null element failures in string array tests.
* Fixed inconsistent deploy configuration naming.
* Fixed `StringIndexOutOfBoundsException` with unfound table methods.

### Demo

* Fixed `-p` folder creation on Windows.
* Fixed duplicate running process icons on macOS.
* Fixed non-functional "RESTful Rule Services" link.
* Removed non-functional `stop.cmd` script.

### Demo, WebStudio

* Fixed `SetNonZeroValuesTest` result failures in "Example 1 - Bank Rating."

### Demo, Docker

* Fixed non-functional force-recreate script.

### Rule Services

* Fixed project deployment `NullPointerException` with `custom.spreadsheet.type = false`.
* Fixed missing 200/204 responses in OpenAPI with `@Operation` annotation.
* Fixed incorrect default value for `array` type in OpenAPI schema.

### Repository

* Fixed unresponsive local Git repository after remote tag repush.

### Maven Plugin

* Fixed incompatibility with Maven 3.5.4.

## Library Updates

| Library                | Version      |
|:-----------------------|:-------------|
| Spring Framework       | 5.3.24       |
| Spring Boot            | 2.7.7        |
| Spring Security        | 5.8.1        |
| Swagger UI             | 4.15.5       |
| Log4j                  | 2.19.0       |
| Cassandra Driver       | 4.15.0       |
| Thrift                 | 0.17.0       |
| Netty                  | 4.1.86.Final |
| Reactor Netty          | 1.1.1        |
| Kotlin StdLib          | 1.7.22       |
| Jackson                | 2.14.1       |
| Jetty                  | 10.0.13      |
| Amazon AWS SDK         | 1.12.376     |
| Azure Blob Storage SDK | 12.20.1      |
| MSSQL Driver           | 11.2.2.jre11 |
| Hibernate              | 5.6.14.Final |
| Mockito                | 4.9.0        |
| Datasource Proxy       | 1.8.1        |
| Testcontainers         | 1.17.6       |
| GreenMail              | 1.6.12       |
| Commons Compress       | 1.22         |
| Gson                   | 2.10         |
| Groovy                 | 3.0.14       |
| POI                    | 5.2.3        |
| BouncyCastle           | 1.72         |
| Joda Time              | 2.12.2       |
| SnakeYAML              | 1.33         |
| CXF                    | 3.5.5        |
| Woodstox               | 6.4.0        |

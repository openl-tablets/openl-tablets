---
title: OpenL Tablets 5.26.1 Release Notes
date: 2022-08-03
description: Localization bundle support, template messaging function, AspectJ profiler, varargs in array
    functions, context data as separate argument, extensive bug fixes, and library updates.
---

OpenL Tablets **5.26.1** introduces localization bundle support for properties files, a template messaging function, AspectJ OpenL profiler, varargs support in array functions, and context data as a separate argument. The release also includes extensive bug fixes and library updates.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## New Features

### Localization Bundle Support for Properties Files

Properties files serving as localization bundles must be located in the `i18n` folder within OpenL Tablets projects. The naming convention requires: `message%locale%.properties`. These key-value text files are accessed via the `msg(String code, Object... params)` function.

### Template Function for Messaging

A new `format(String pattern, String args)` function enables pattern-based string substitution:

* Example: `format("Hello 1 and 2", "John", "Hanna")` produces `"Hello John and Hanna"`

### REST Service for application.properties Download

Two endpoints provide default `application.properties` configurations:

* WebStudio: `GET /rest/config/application.properties`
* Rule Services: `GET /admin/config/application.properties`

### AspectJ OpenL Profiler

The profiler collects method call statistics during request execution in OpenL Rule Service.

### Varargs Support in Array Functions

The following functions now support variable arguments:

* `T[] add(T... elements)`
* `T[] add(T[] target, T... elements)`
* `T[] addAll(T[]... elements)`
* `T[] removeElement(T[] target, T... elements)`
* `T[] removeNulls(T... elements)`
* `T[] sort(T... elements)`
* `Boolean allTrue(Boolean... elements)`
* `Boolean anyTrue(Boolean... elements)`
* `Boolean allFalse(Boolean... elements)`
* `Boolean anyFalse(Boolean... elements)`
* `Boolean noNulls(T... elements)`
* `T max(T... elements)`
* `T min(T... elements)`
* `T avg(T... elements)`
* `T sum(T... elements)`
* `T product(T... elements)`

### Context Data as Separate Argument

Context data may now be passed as an independent argument in OpenL Tablets rules.

## Improvements

### Core and Rule Services

* The `error()` function now supports error codes:
    * Format: `error("ERRCD1", "Error Description1")`
    * Output: `{ "code": "ERRCD1", "message": "Error Description1", "type": "USER_ERROR"}`

### Core

* `Date()` function and supplementary date manipulation functions introduced.
* Reduced memory usage in `DecisionTableMetaInfoReader`.
* Thai Baht (THB) currency support added.

### Rule Services

* Automatic deployment of `classpath:*/openl/*.zip` resources to repository.

### WebStudio

* `index.lock` file deletion on Git repository initialization.
* Deployment repositories now optional.

### Demo and Docker

* Heroku container deployment support added.
* Jetty upgraded to version 10 with reduced module count.

### WebStudio and Rule Services

* Default fallback values for repeated configurations supported.

## Bug Fixes

### Core

* Fixed `StringRange` `contains()` function matching for ranges beginning at zero.
* Fixed incorrect error/warning messages for versioned tables with CustomSpreadsheetResult parameters.
* Fixed spreadsheet compilation failures from line breaks in titles.
* Fixed missing error display in test tables referencing SpreadsheetResult with CustomSpreadsheetResult columns.
* Fixed incorrect nearest common type identification for mixed-dimension arrays.
* Fixed `NullPointerException` on opening smart rules tables with merged cells.
* Fixed incorrect text highlighting in error messages for rules starting with numbers.
* Fixed non-informative error messages for misused OpenL keywords.
* Fixed missing error validation for arguments exceeding input parameter count.
* Fixed improper SpreadsheetResult casting by full name from other modules.
* Fixed `NullPointerException` errors for constructors with datatype errors.
* Fixed `copy()` operation failure for transient object fields.

### WebStudio

* Fixed delete action button corruption during project deletion.
* Fixed circular reference compilation errors for specific tables.
* Fixed session cookies missing `HttpOnly` and `Secure` attributes.
* Fixed "Something went wrong" error on CustomSpreadsheetResult expansion.
* Fixed JNDI injection vulnerability in `RepositoryTreeController`.
* Fixed run table execution errors.
* Fixed merged row table opening failures.
* Fixed Open/Close button display for local projects after repository path updates.
* Fixed user record updates/deletions with special characters.
* Fixed "Constructor not found" errors in dependent tables.
* Fixed uncloseable pop-up windows on browser back button.
* Fixed system virtual tables appearing in search results.
* Fixed login failures for users with extended role names.
* Fixed Excel file corruption during function editing.
* Fixed automatic "Deployment1" naming for initial repositories.
* Fixed field length constraints for "Default group for all users:" setting.
* Fixed Open button access restrictions for Viewer-role users.
* Fixed 200KB+ request handling errors.
* Fixed Deploy button display for unauthorized users on local projects.
* Fixed project opening delays with many parameters.
* Fixed missing circular dependency error display.
* Fixed design repository accessibility UI issues.
* Fixed project copy failures with "Do not link to origin" option.
* Fixed XSS vulnerabilities in Group name field.
* Fixed Git URL field default JDBC population.
* Fixed incorrect module display in breadcrumbs.
* Fixed datatype table link display errors.
* Fixed missing warnings for same-named project openings.
* Fixed unspecified error messages for invalid workspace directories.

### Demo

* Fixed warning messages in Tutorial 6 and Tutorial 8.
* Fixed `java.security.AccessControlException` on project creation.
* Fixed `AccessControlException` errors during concurrent execution with Java security policy.
* Fixed proxy prefixed location path issues.

### Demo, Docker

* Fixed `JAVA_OPTS` memory settings redefinition failures.

### Rule Services

* Fixed Tomcat log memory leaks on stopping.
* Fixed null `message` attribute type misidentification in 422 responses.
* Fixed accidental deadlocks during deployment repository deployments.
* Fixed unexpected server error 500 `IllegalArgumentException` occurrences.

### Security

* Fixed SAML attributes not applying when `SAMLResponse` lacks `xsi:type="xs:string"`.

### Docker

* Fixed non-Latin symbol compatibility failures.

## Library Updates

| Library                | Version                  |
|:-----------------------|:-------------------------|
| Spring Framework       | 5.3.22                   |
| Spring Security        | 5.7.2                    |
| Spring Boot            | 2.7.1                    |
| Kafka Clients          | 3.2.0                    |
| CXF                    | 3.5.3                    |
| Cassandra Driver       | 4.14.1                   |
| Log4j                  | 2.18.0                   |
| Jetty                  | 10.0.11                  |
| Hive JDBC              | 3.1.3                    |
| Avro                   | 1.11.0                   |
| ZooKeeper              | 3.7.1                    |
| Thrift                 | 0.16.0                   |
| Netty                  | 4.1.79.Final             |
| Kotlin StdLib          | 1.7.10                   |
| Groovy                 | 3.0.11                   |
| Amazon AWS SDK         | 1.12.262                 |
| Azure Blob Storage SDK | 12.18.0                  |
| JGit                   | 6.2.0.202206071550-openl |
| Mockito                | 4.6.1                    |
| Joda Time              | 2.10.14                  |
| Guava                  | 31.1-jre                 |
| XMLSecurity            | 2.3.1                    |
| Swagger UI             | 4.11.1                   |
| Istack Commons Runtime | 3.0.12                   |
| H2 Database            | 2.1.214                  |
| Hibernate              | 5.6.10.Final             |
| Hibernate Validator    | 6.2.3.Final              |
| Reactor Netty          | 1.0.21                   |
| OkHttp                 | 4.10.0                   |
| Reactive Streams       | 1.0.4                    |
| Datasource Proxy       | 1.8                      |

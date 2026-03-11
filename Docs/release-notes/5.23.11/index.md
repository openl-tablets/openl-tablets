---
title: OpenL Tablets 5.23.11 Release Notes
date: 2021-02-10
description: Release with improvements to the ternary operator, empty result processing, compilation performance,
    and Rule Services repository, plus 25 bug fixes and library updates.
---

OpenL Tablets **5.23.11** includes improvements, bug fixes, and library updates.

## Improvements

**Core:**

* The `:b` part is now optional in the ternary operation `(condition)? a : b`. If `:b` is absent, `null` is returned
  when the condition is false.
* A new property `emptyResultProcessing` is introduced for controlling rules behavior when the return value is empty.
* Compilation performance is improved.

**Rule Services:**

* A new default repository type `repo-jar` is introduced, allowing deployment of JAR and ZIP archives without creating
  local copies of the projects.

## Bug Fixes

**WebStudio:**

* Fixed: A white screen on the Repository tab is displayed if two projects with dependencies on each other are closed.
* Fixed: The "Receive their updates" button is active after the first merge.
* Fixed: An incorrect message is displayed on the "Confirm Delete" pop-up for Deploy Configuration.
* Fixed: Two modules with the same path can be created.
* Fixed: WebStudio cannot open projects due to an exception in the binding phase.
* Fixed: WebStudio hung threads when using the Trace feature.

**Core:**

* Fixed: An NPE error is presented to the user if a Java keyword is typed as the Return type.
* Fixed: A module fails to load with an NPE error.
* Fixed: The precision in the test table does not work if there is another module having a table with the same name.
* Fixed: A Smart Lookup table compilation failure.
* Fixed: A Smart Lookup table is transposed during compilation if there is a pair of header cells where the left cell
  has height = 2 and the right cell has height = 1.
* Fixed: A Smart Lookup table of a certain structure is wrongly compiled as transposed.
* Fixed: The build fails sometimes on Windows servers due to an "OutOfMemory Java heap space" error.
* Fixed: An NPE error is presented to the user in a Smart Lookup table when the system tries to compile a transposed
  version of the table.
* Fixed: The error "Cannot build an evaluator" is presented to the user if external condition parameters are not used in
  the External condition expression.
* Fixed: An error is displayed for a Constant table after adding properties.
* Fixed: An NPE error is displayed on opening a table with a reference to a `SpreadsheetResult` null field.
* Fixed: `ArrayIndexOutOfBoundsException` is presented to the user from Smart Rules with merged conditions and custom
  input and output parameters.
* Fixed: Slow compilation performance when many similar spreadsheets are used.
* Fixed: The Trace and Run features ignore the runtime context when business versions are stored in different modules in
  multi-module mode.
* Fixed: `ArrayIndexOutOfBoundsException` is presented to the user if cells are merged in External Conditions.
* Fixed: A `State` property is added to the table instead of being inherited.

**Rule Services:**

* Fixed: A successful operation response is not generated in OpenAPI for paths if the returned result is a
  two-dimensional array.

**Rule Repository:**

* Fixed: A confusing error message is displayed when connecting to a GitLab repository.

**Maven Plugin:**

* Fixed: The Maven plugin does not truncate a ZIP artifact file, so it could not be read by `ZipFile`.

## Library Updates

| Library                   | Version          |
|:--------------------------|:-----------------|
| CXF                       | 3.4.2            |
| Log4j                     | 2.14.0           |
| Jetty                     | 9.4.36.v20210114 |
| Kafka                     | 2.7.0            |
| Bouncy Castle             | 1.68             |
| Joda-Time                 | 2.10.9           |
| Spring Data Elasticsearch | 3.2.12.RELEASE   |
| Jakarta WS RS API         | 2.1.6            |
| Jakarta Validation API    | 2.0.2            |
| Jakarta Annotation API    | 1.3.5            |
| Swagger V3 Annotations    | 2.1.3            |

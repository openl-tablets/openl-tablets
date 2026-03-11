---
title: OpenL Tablets 5.22.2 Release Notes
date: 2019-06-28
description: Release with multiple project deployment, Git branch support in REST services, Min/Max columns for Smart Rules,
    Tomcat 9 support, 45 bug fixes, and library updates including Spring Framework 4.3.24 and Jakarta namespace migration.
---

OpenL Tablets **5.22.2** includes improvements, bug fixes, breaking changes, and library updates.

## Improvements

* Multiple project deployment support.
* Time elapsed and memory used for compilation of rules are displayed in the Maven plugin.
* An error message is displayed when `OpenLUserRuntimeException` is thrown on tests.
* Synchronization on rules update is added.
* Cache in `OpenMethodDispatcher` is reworked to improve performance.
* Unpacking rules from `.jar` files under JBoss EAP 7.2 is now supported.
* Branches in the REST service for the Design repository are now supported.
* Comments are generated using the predefined template on the Create Project page.
* The selected branch is now available at all levels in Breadcrumbs.
* A link to external conditions, actions, and returns is added to Smart Rules.
* The "Internal User" field is renamed to "Local User" in the administration.
* Git Repository configuration is now supported for development sources.
* Excel files are sorted alphabetically in the "Select Excel file" drop-down.
* Project status is changed to "No Changes" on copying a project to a branch.
* Hints are displayed with 3 rows for column titles in Simple and Smart Rules.
* The committer's login can be added to a comment for the Git repository.
* Default configuration can now be loaded from `application-default.properties`.
* Hints and links to datatypes, decision tables, and constants in conditions are added.
* Apache Tomcat 9 is supported.
* Null elements usage can be configured for calculations.
* Min and Max columns for Smart Rules are added and can be used as a range.
* Several returns and multiline titles are supported for Smart Rules.
* The `contains` function is improved to search for a value in a range.
* Range arrays are supported in Smart Rules.
* Logic of column type identification is improved for Smart Rules.
* Identification logic is improved for detecting whether a value is within a string range.
* New comparison operators are developed for strings to support comparison logic.
* The order of matching input values to horizontal conditions is modified.

## Bug Fixes

* Fixed: Reference to another data table does not work in a data table.
* Fixed: Date range allows entering an invalid time.
* Fixed: The "Type not present" error is displayed in a datatype table with a custom field.
* Fixed: Ranges like `<=a` and `>=a` without spaces do not work for string ranges.
* Fixed: For Smart and Simple rules, an incorrect condition type is displayed in the hint.
* Fixed: A transposed data table is not compiled properly with a reference to a data table.
* Fixed: A whole Spreadsheet result is returned as the actual value instead of a single cell.
* Fixed: Issues with String Ranges in Smart Rules.
* Fixed: Spaces in a string range must be ignored.
* Fixed: Instances in a data table with merged cells are calculated incorrectly.
* Fixed: A sub-instance is incorrectly assigned to an instance with incorrect data fields.
* Fixed: An array cannot be specified in a data table by merging two columns.
* Fixed: An incorrect message is displayed if a table lacks data for conditions.
* Fixed: Comparison of arrays does not work in decision tables.
* Fixed: The sorting command in an expression changes the order of array elements.
* Fixed: An error message is displayed for a project with dependencies on another project.
* Fixed: Date range does not work for Smart Rules.
* Fixed: Memory is allocated incorrectly for Demo under Linux.
* Fixed: Incorrect hints are displayed for SmartLookup and SimpleLookup starting at row two.
* Fixed: Trace functionality is hanging.
* Fixed: The "Internal user" column should be available only for Active Directory mode.
* Fixed: Comment validation does not work in a copied project.
* Fixed: An "Internal Server Error" is displayed when opening a project from Deploy Configuration.
* Fixed: Local Git commits must be discarded if pushing failed.
* Fixed: A user can copy a project into an existing branch.
* Fixed: Repository UI is broken if a user message pattern is specified.
* Fixed: The name of a copied project is not changed in the `rules.xml` file.
* Fixed: No comment is generated when creating a project from a template.
* Fixed: The range UI appears when editing a range array value in decision tables.
* Fixed: A constant is highlighted incorrectly in a condition with other values.
* Fixed: The "element is null" error is displayed in a data table.
* Fixed: The list of modules is not refreshed after switching to another branch.
* Fixed: An incorrect version of `rules-deploy.xml` is displayed on opening the previous version.
* Fixed: A new branch is always created from `master`, not the currently selected branch.
* Fixed: "Internal Server Error" occurs if the user deploys from a branch with no project.
* Fixed: Project information is not updated after the branch is changed in the Editor.
* Fixed: `IllegalArgumentException` is displayed for an unsupported placeholder.
* Fixed: An incorrect committer is populated when JGit merges automatically.
* Fixed: No warning is displayed about potential changes lost when creating a new branch.
* Fixed: An OpenL rules table throws `NullPointerException` if the table is empty.
* Fixed: OpenL Tablets WebStudio is not working with `HttpHeaderSecurityFilter`.
* Fixed: Incorrect text is displayed in the validation message for "Changes check interval".
* Fixed: `URISyntaxException` on starting Tomcat.
* Fixed: Swagger does not work on WildFly.
* Fixed: Some transitive dependencies appear in `rules.zip` after the build.
* Fixed: A memory leak occurs for rules with many Equals conditions.

## Breaking Changes

The behavior for `null` values in multiplication and division operations has changed. To restore the legacy behavior,
add the following import statement to your rules:

```java
import org.openl.rules.binding.MulDivNullToOneOperators.*;
```

## Library Updates

| Library                | Version        |
|:-----------------------|:---------------|
| Spring Framework       | 4.3.24.RELEASE |
| Spring Security        | 4.2.13.RELEASE |
| CXF                    | 3.3.2          |
| Jackson                | 2.9.9          |
| Joda-Time              | 2.10.2         |
| Ehcache                | 2.10.6         |
| Swagger UI             | 3.22.2         |
| Swagger JAXRS          | 1.5.22         |
| SLF4J                  | 1.7.26         |
| HikariCP               | 3.3.1          |
| AWS SDK                | 1.11.580       |
| XStream                | 1.4.11.1       |
| Commons Lang           | 3.9            |
| cglib                  | 3.2.12         |
| H2 DB                  | 1.4.199        |
| Microsoft JDBC Driver  | 7.2.2.jre8     |
| HttpClient             | 4.5.9          |
| istack-commons-runtime | 3.0.8          |
| JCodeModel             | 3.2.3          |
| SnakeYAML              | 1.24           |
| plexus-archiver        | 4.1.0          |
| Jakarta XML Bind API   | 2.3.2          |
| Jakarta Annotation API | 1.3.4          |
| Jakarta Activation API | 1.2.1          |
| Jakarta XML WS API     | 2.3.2          |
| cloning                | 1.9.12         |
| Javassist              | **deleted**    |
| javax.jws:jsr181-api   | **deleted**    |

---
title: OpenL Tablets 5.26.7 Release Notes
date: 2023-04-14
description: AWS S3 server-side encryption, HTTP authorization customization in Rule Services,
    multiple bug fixes, and library updates.
---

OpenL Tablets **5.26.7** is a maintenance release introducing AWS S3 server-side encryption support, HTTP authorization customization in Rule Services, multiple bug fixes, and library updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)
* [Known Issues](#known-issues)

## Improvements

### Repository

* Server-side encryption feature support added for AWS S3 repository.

### Rule Services

* HTTP authorization customization now available in Rule Services.

## Bug Fixes

### WebStudio

* Corrected various grammatical errors.
* Success message now displays when saving projects in the Editor.
* Previously deleted projects no longer appear in Workspace after repository type changes.
* "Comment" field now displays on Create Project screens for local projects when customized comments are enabled.
* Fixed error `Failure in the method 'org.openl.rules.helpers.RulesUtils.copy(java.lang.Object)'` during test runs after module updates.
* Fixed incorrect date display in spreadsheet test results when cells reference Date array elements.

### Rule Services

* Fixed combined spreadsheet type generation.

### Core

* Warning now displays for `caseInsensitive` parameters.
* Return cell expressions no longer execute when a rule is unmatched.
* Fixed "Ambiguous dispatch" error now properly presented when a rule is versioned by 2 properties with even versioned table counts.

### Maven Plugin

* Fixed memory leak in OpenL Maven plugin caused by Log4j in `WebAppClassLoader`.

## Library Updates

| Library                | Version      |
|:-----------------------|:-------------|
| Spring Framework       | 5.3.26       |
| Spring Boot            | 2.7.10       |
| Jose4j                 | 0.9.3        |
| JSON Smart             | 2.4.10       |
| OpenSAML               | 4.3.0        |
| ASM                    | 9.5          |
| Thrift                 | 0.18.1       |
| Netty                  | 4.1.91.Final |
| Reactor Netty          | 1.1.6        |
| Amazon AWS SDK         | 1.12.447     |
| Azure Blob Storage SDK | 12.21.1      |
| Jaxb Runtime           | 2.3.8        |
| GreenMail              | 1.6.14       |
| Commons Compress       | 1.23.0       |
| Gson                   | 2.10.1       |
| BouncyCastle           | 1.73         |
| Groovy                 | 3.0.17       |
| Joda Time              | 2.12.5       |
| XMLSecurity            | 2.3.3        |

## Known Issues

* Rule Services startup fails when the property `log.request-id.header` is configured.

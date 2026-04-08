---
title: OpenL Tablets 5.25.3 Release Notes
date: 2021-12-09
description: Added U.S. territory support, GZIP encoding, Forwarded header support, deprecated *Value types and SOAP/WSDL services, and fixed 3 bugs in the core engine and WebStudio.
---

OpenL Tablets **5.25.3** is a maintenance release that adds U.S. territory support, GZIP encoding, Forwarded header
support, deprecates `*Value` types and SOAP/WSDL services, and resolves 3 bugs.

## Improvements

### Core

* Added support for U.S. territories: American Samoa, Guam, Northern Mariana Islands, and Virgin Islands.
* Added the ability to toggle CXF statistics collection.
* Added GZIP encoding support for requests and responses.

### WebStudio

* Added support for `Forwarded` and `X-Forwarded-*` headers.

## Bug Fixes

### Core

* Fixed horizontal conditions being matched incorrectly with input parameters when a column contains empty values in a
  smart lookup table.
* Fixed no error message being displayed when an alias datatype itself is passed as a parameter instead of a variable.

### WebStudio

* Fixed WebStudio becoming unresponsive when a user opens a project that has a circular dependency.

## Deprecations

### Core

* The `*Value` types in the `org.openl.meta` package are deprecated. Use the corresponding Java types as replacements.

### Rule Services

* **SOAP/WSDL services** are deprecated. Migrate to REST/OpenAPI v3 services.
* **Aegis converters** are deprecated. Migrate to REST/OpenAPI v3 services.

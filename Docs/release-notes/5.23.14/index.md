---
title: OpenL Tablets 5.23.14 Release Notes
date: 2021-12-15
description: Maintenance release with bug fixes for the Flatten function, condition evaluation,
    the deprecated include feature, and the OpenL Maven plugin, plus Hibernate and Log4j library updates.
---

OpenL Tablets **5.23.14** is a maintenance release containing bug fixes and library updates.

## Bug Fixes

* Fixed: The `Flatten` function with Datatype arrays returns `Object[]` rather than the expected `MyDatatype[]`.
* Fixed: Return values appear in results even when conditions are not satisfied.
* Fixed: The deprecated `include` feature ceased functioning for dependent modules.
* Fixed: OpenL Maven plugin enters an infinite loop when executing dual package goals within one build.

## Library Updates

| Library             | Version      |
|:--------------------|:-------------|
| Log4j               | 2.15.0       |
| Hibernate           | 5.4.24.Final |
| Hibernate Validator | 5.4.3.Final  |

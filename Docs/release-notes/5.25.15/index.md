---
title: OpenL Tablets 5.25.15 Release Notes
date: 2022-11-10
description: Deprecated the VariationPack utilities component and comprehensive library updates for improved security and stability
---

OpenL Tablets **5.25.15** is a maintenance release that deprecates the VariationPack utilities component and includes
library updates.

## Deprecations

* The **VariationPack** utilities component has been deprecated. Consider one of the following alternatives:
    * Develop an OpenL rule to handle variation calculations.
    * Implement a custom method interceptor to prepare input data sets for variation operations.

## Library Updates

| Library          | Version |
|:-----------------|:--------|
| Spring Framework | 5.3.23  |
| Spring Security  | 5.7.5   |
| Spring Boot      | 2.7.5   |
| Apache CXF       | 3.5.4   |
| Jackson Core     | 2.14.0  |
| SnakeYAML        | 1.33    |
| Woodstox         | 6.4.0   |

---
title: OpenL Tablets 5.12.0 Migration Notes
---

## Eclipse Plugins Removed

Eclipse plugins have been discontinued and deleted. Use the OpenL Maven Plugin instead for generating interfaces,
validating, and testing rules.

## WebStudio Repository Dependencies

WebStudio Repository dependencies are no longer functional. Configure new project dependencies via the Project page in
the Rules Editor.

## Data Table Duplicate Names

Data tables do not support versioning. If a Data table with the same name needs to be defined in several modules,
replace it with an equivalent SimpleRules table. Duplicate Data table names now raise compilation exceptions instead of
defaulting to the root module.

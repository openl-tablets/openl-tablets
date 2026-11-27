---
title: "OpenL Tablets 7.2.0 Migration Notes"
---

A fix of the rules engine changes how an existing project names its modules, as listed for the rules authors.

## Rules Authors

* **Workbooks of one name in different folders are modules of their own.** When `rules/FL/calc.xlsx` and
  `rules/NY/calc.xlsx` are read as modules by a pattern, by default or without a declared name, they are `calc (FL)`
  and `calc (NY)`: the last folders of the path, as few as tell them apart, follow the name in brackets. Before, both
  were `calc`, and only one of them was compiled, tested and depended on, without a message. An `Environment` table
  that depends on `calc` finds no module now and stops with `Dependency 'calc' is not found.`: write `calc*` to
  depend on every version. Two modules declared under one name in `rules.xml` stop the project with
  `Several modules are named 'calc': 'FL/calc.xlsx', 'NY/calc.xlsx'.`: give each module a name of its own.

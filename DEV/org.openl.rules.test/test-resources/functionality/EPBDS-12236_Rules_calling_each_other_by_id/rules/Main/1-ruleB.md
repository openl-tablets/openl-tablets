---
table: SmartRules SpreadsheetResult ruleB(Integer param)
properties:
  id: bById
---

> The rule is compiled first, so it is in the module by its id when ruleA calls it.

| param | Result            |
|-------|-------------------|
| 1     | =mySr1(param)     |
|       | =ruleA(param - 1) |

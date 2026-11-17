---
table: SmartRules SpreadsheetResult myVersionedRules(Integer param)
properties:
  startRequestDate: 2021-03-03
---

> The version applying from March 3, 2021. Its call of the rule is dispatched again, to this version.

| param | Result                       |
|-------|------------------------------|
| 1     | =mySr2(param)                |
|       | =myVersionedRules(param - 1) |

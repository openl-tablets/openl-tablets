---
table: SmartRules SpreadsheetResult ruleA(Integer param)
---

> The united spreadsheet result of ruleA takes the steps of ruleB, which takes the steps of ruleA.

| param | Result            |
|-------|-------------------|
| 1     | =mySr1(param)     |
|       | =ruleB(param - 1) |

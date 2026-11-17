---
table: SmartRules SpreadsheetResult ruleA(Integer param)
---

> The united spreadsheet result of ruleA takes the steps of sprB, whose step is the result of ruleA.

| param | Result           |
|-------|------------------|
| 1     | =mySr1(param)    |
|       | =sprB(param - 1) |

---
table: SmartRules SpreadsheetResult myRules(Integer param)
---

> The rule calls itself: the call adds no steps to the united spreadsheet result of the rule.

| param | Result              |
|-------|---------------------|
| 1     | =mySr1(param)       |
| 2     | =mySr2(param)       |
|       | =myRules(param - 1) |

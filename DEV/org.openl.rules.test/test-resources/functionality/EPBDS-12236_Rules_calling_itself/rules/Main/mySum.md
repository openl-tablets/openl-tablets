---
table: SmartRules SpreadsheetResult mySum(Integer param, Integer total)
---

> Each call adds its param to the total and calls the rule again, until param is 0.

| param | Result                            |
|-------|-----------------------------------|
| 0     | =mySr2(total)                     |
|       | =mySum(param - 1, total + param)  |

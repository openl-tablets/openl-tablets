---
table: SmartRules SpreadsheetResult myTotal(Integer param)
---

> The steps of the rule come first in the united spreadsheet result, though the rule adds them last. It reads a step
> of the Spreadsheet, which the united spreadsheet result has already.

| param | Result                         |
|-------|--------------------------------|
| 1     | =mySr1(param)                  |
|       | =mySr1(myTotal(param, 1).$Sum) |

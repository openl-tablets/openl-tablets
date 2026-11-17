---
table: SmartRules SpreadsheetResult ruleA(Integer param)
---

> The rule calls ruleB by its id while ruleB builds its type, which a call by the name of ruleB does not take either.

| param | Result            |
|-------|-------------------|
| 1     | =mySr1(param)     |
|       | =bById(param - 1) |

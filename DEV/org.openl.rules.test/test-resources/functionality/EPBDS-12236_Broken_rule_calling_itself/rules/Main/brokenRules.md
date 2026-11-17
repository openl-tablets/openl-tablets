---
table: SmartRules SpreadsheetResult brokenRules(Integer param)
---

> No parameter matches the condition title, so the rule is not compiled and its error is reported on it.

| plan | Result                 |
|------|------------------------|
| 1    | =mySr1(param)          |
|      | =brokenRules(param - 1) |

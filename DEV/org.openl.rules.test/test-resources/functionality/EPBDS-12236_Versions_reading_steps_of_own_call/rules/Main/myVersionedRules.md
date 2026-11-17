---
table: SmartRules SpreadsheetResult myVersionedRules(Integer param)
---

> The version reads the step the other version brings to the type, which is not complete while the rule is compiled.

| param | Result                                     |
|-------|--------------------------------------------|
| 1     | =mySr1(param)                              |
|       | =mySr1(myVersionedRules(param - 1).$Step2) |

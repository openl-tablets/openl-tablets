---
table: SmartRules SpreadsheetResult myVersionedRules(Integer param)
properties:
  startRequestDate: 2021-03-03
---

> The version reads the step the other version brings to the type: it fails the same way whichever is bound first.

| param | Result                                     |
|-------|--------------------------------------------|
| 1     | =mySr2(param)                              |
|       | =mySr2(myVersionedRules(param - 1).$Step1) |

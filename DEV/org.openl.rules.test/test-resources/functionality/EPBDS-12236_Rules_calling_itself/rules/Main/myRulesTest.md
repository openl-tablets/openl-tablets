---
table: Test myRules myRulesTest
---

> Steps of the spreadsheets the rule returns are read without a cast, the recursive call included.

| param | _res_.$Step1 | _res_.$Step2 |
|-------|--------------|--------------|
| Param | Step1        | Step2        |
| 1     | 5            |              |
| 2     |              | 20           |
| 4     |              | 20           |

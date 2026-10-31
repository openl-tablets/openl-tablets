---
table: Rules MyObject myRule()
merges: [C1:D1, C2:D2]
---

> Markdown copy of `EPBDS-13605_DT_Execution.xlsx`: a decision table whose return spans two parameters.

| Rule     | C1                                          | RET1                  |                |
|----------|---------------------------------------------|-----------------------|----------------|
|          | `p2.toString().trim().equals("") \|\| true` | new MyObject(p1,  p2) |                |
|          | Boolean p                                   | String p1             | Double p2      |
| Rule num | My Conditon                                 |                       |                |
| R1       | true                                        | test                  |                |
| R2       | false                                       |                       | =MyData[2]=3;2 |

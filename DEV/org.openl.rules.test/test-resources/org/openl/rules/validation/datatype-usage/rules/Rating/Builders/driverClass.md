---
table: ColumnMatch <MATCH> String driverClass(Driver driver)
merges:
  - C1:D1
  - C2:D2
---

Classes a driver by the rating: the name of the row reads the field of the argument.

| names         | operation | values |        |
|---------------|-----------|--------|--------|
| Rating        | Condition | Class  |        |
| Return Values |           | Senior | Junior |
| rating        | min       | 10     | 0      |

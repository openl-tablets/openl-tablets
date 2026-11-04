---
table: Spreadsheet SpreadsheetResult premium(Driver driver)
---

| Step      | Value                         |
|-----------|-------------------------------|
| Base      | `= basePremium(driver.age)`   |
| AgeFactor | `= driver.age < 25 ? 1.5 : 1` |
| Total     | `= $Base * $AgeFactor`        |

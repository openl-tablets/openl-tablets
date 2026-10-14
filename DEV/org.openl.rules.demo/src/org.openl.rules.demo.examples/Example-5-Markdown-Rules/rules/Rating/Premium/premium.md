---
table: Spreadsheet SpreadsheetResult premium(Policy policy)
description: Yearly premium of a policy
---

A formula is wrapped in backticks only for GitHub to show it as it is: the backticks are removed, and `*` stays a
multiplication rather than turning the text italic.

| Step      | Value                                                  |
|-----------|--------------------------------------------------------|
| BaseRate  | `= baseRate(policy.state, policy.vehicle.type)`        |
| AgeFactor | `= ageFactor(policy.driver.age)`                       |
| Discount  | `= experienceDiscount(policy.driver.licenseYears)`     |
| Premium   | `= round($BaseRate * $AgeFactor * (1 - $Discount), 2)` |

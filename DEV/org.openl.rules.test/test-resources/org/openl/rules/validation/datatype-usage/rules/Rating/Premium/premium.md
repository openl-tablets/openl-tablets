---
table: Spreadsheet SpreadsheetResult premium(Policy policy)
---

The yearly premium of a policy.

| Step    | Value                                        |
|---------|----------------------------------------------|
| Novices | `= policy.drivers[(d) @ d.licenseYears < 3]` |
| Risky   | `= policy.drivers[@ points > 3]`             |
| Factor  | `= ageFactor(policy.drivers[0])`             |
| Year    | `= policy.vehicle.year`                      |
| Rate    | `= stateRate(policy.state)`                  |
| Premium | `= $Rate.amount * $Factor`                   |

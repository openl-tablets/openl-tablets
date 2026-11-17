---
table: SmartRules SpreadsheetResult myPremium(Integer amount, String plan)
---

> The rule calls the Spreadsheet of its name (EPBDS-12512).

| plan | Result                  |
|------|-------------------------|
| Gold | =myPremium(amount * 10) |
|      | =myPremium(amount)      |

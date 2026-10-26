---
table: Spreadsheet SpreadsheetResult FigureTypes(Double weight)
---

| Step | Value |
|---|---|
| Figures | = ShippingFigures(weight) |
| DaysInTransit | = TypeOf($Figures.$Formula$DaysInTransit) |
| Express | = TypeOf($Figures.$Formula$Express) |
| Dispatched | = TypeOf($Figures.$Formula$Dispatched) |
| Cost | = TypeOf($Figures.$Formula$Cost) |

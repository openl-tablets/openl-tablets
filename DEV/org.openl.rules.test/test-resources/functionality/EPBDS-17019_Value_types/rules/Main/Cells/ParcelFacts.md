---
table: Spreadsheet SpreadsheetResult ParcelFacts()
---

> A value takes the type a cell of a workbook gives it: a whole number, a boolean and a date. A text that is not written
> back as the same value stays a text.

| Step | Value | Type |
|---|---|---|
| Pieces | 3 | = TypeOf($Value) |
| Below | -3 | = TypeOf($Value) |
| Weight | 1.5 | = TypeOf($Value) |
| Label | Fragile | = TypeOf($Value) |
| Insured | true | = TypeOf($Value) |
| Shipped | 2026-03-02 | = TypeOf($Value) |
| Sent | 2026-03-02T10:30:15 | = TypeOf($Value) |
| Loud | TRUE | = TypeOf($Value) |
| NotADay | 2026-13-45 | = TypeOf($Value) |
| Ages | 18-65 | = TypeOf($Value) |
| Huge | 2147483648 | = TypeOf($Value) |

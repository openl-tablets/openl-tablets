# Every cell of a vocabulary column is edited with its list of values

`VocabularFail.xlsx` is the workbook of the ticket. Its SimpleRules `CorporateRating` takes a `RiskGroup`, a vocabulary
of `LOW`, `HIGH` and `MEDUIUM`. The column `riskOfWorkWithCorporate` holds `LOW`, `123`, `-3`, `3` and `4`, so the
table does not compile. The compiler also reads it transposed, then drops that reading and keeps the errors of the
first one. The table used to keep the rows of the dropped reading, so the cells `-3`, `3` and `4` were edited with a
number field.

- **`010-setup`** — creates the project from the Excel file and opens it, as **New Project** > **From Excel files**
  does.
- **`020-edit-the-table`** — the raw read gives the values of the table, and the editors give how its cells are
  written. The column `riskOfWorkWithCorporate` is one area under its title edited with the list of `RiskGroup`
  values, whatever its cells hold; `financialRating` and the return column are numbers.

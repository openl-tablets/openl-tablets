# EPBDS-17089 — a cell holding an Excel error reads as the error Excel shows

A cell whose Excel formula computed an error, such as `=C4/0`, read as `unknown type: FORMULA` once OpenL Studio had
the workbook open for writing, and as an empty cell before. The compilation of the module read it as empty too, so
the error was never told. Now such a cell, and an error typed into a cell, read as the text Excel shows: `#DIV/0!`,
`#N/A`, `#REF!` and so on.

`EPBDS-17089.zip` is a project written for this. The Spreadsheet `Errors` has the steps `Base`, `10`, and `Ratio`
`=C4/0`, `Missing` `=NA()` and `Broken`, which holds `#REF!` typed in, all computed by Excel. The Spreadsheet `Ratio`
returns a `Double` whose formula `=1/0` computes an error.

`010-setup` creates the project, opens it, and lists its tables: `Ratio` has an error, `Errors` none.

`020-the-errors-read-as-excel-shows-them`

- `010-read-the-table` — the cells of `Errors` read as `#DIV/0!`, `#N/A` and `#REF!`, each formula beside its value.

`030-a-number-holding-an-error-does-not-compile`

- `010-the-error-names-the-value` — `Ratio` reads with the compilation error of its cell `C12`: `#DIV/0!` is no
  `Double`.

`999-tierdown` closes the project without its changes and erases it.

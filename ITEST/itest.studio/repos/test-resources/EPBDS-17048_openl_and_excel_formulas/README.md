# EPBDS-17048 — an OpenL formula stays a text, an Excel formula is written only when asked for

A value starting with `=` written through the REST API was handed to Excel first, so an OpenL formula that is also
valid Excel, such as `= product($F1:$F4)`, became an Excel formula: Excel multiplied the empty sheet cells `F1` to
`F4`, and OpenL read `0`. Now a value is always written as it is, and an Excel formula is written only from the
`formula` of a cell.

`EPBDS-17048.zip` is a project written for this. The Spreadsheet `CalcProduct` has the steps `F1` to `F4`, holding
the OpenL formulas `= 2`, `= 3`, `= 4` and `= 5`, and returns `= 1`. The Datatype `Blocker` stands one row below it,
so the Spreadsheet has no room to grow in place.

`010-setup` creates the project, opens it, and lists its tables — every one compiling.

`020-an-openl-formula-stays-a-text`

- `010-write-the-product-of-the-steps` — the returned step is set to `= product($F1:$F4)`.
- `020-the-cell-holds-the-text-as-typed` — the cell holds the text with its space, and no Excel formula.
- `030-run` … `050-openl-calculates-120` — OpenL multiplies the steps: `2 * 3 * 4 * 5`.

`030-a-new-excel-formula`

- `010-a-formula-excel-cannot-read-is-refused` — `=SUM(1,` is refused, and nothing is written.
- `015-a-value-beside-a-formula-is-refused` — an edit naming both a value and a formula is refused.
- `020-write-an-excel-formula` — the step `F4` is set to the Excel formula `=2*3`.
- `030-the-cell-holds-the-formula-and-its-value` — the cell reads as the formula and the `6` it computes.
- `040-the-workbook-stores-an-excel-formula` — the workbook stores `2*3` as a formula with its result.
- `050-run` … `070-the-rule-reads-6` — OpenL takes the computed `6`: `2 * 3 * 4 * 6`.

`040-a-moved-table-keeps-its-formulas`

- `010-append-a-row-the-table-has-no-room-for` — a step appended to `CalcProduct`, which moves below `Blocker`.
- `020-the-table-moved` — `CalcProduct` stands at its new place, and nothing fails to compile.
- `030-the-moved-cells-keep-their-formulas` — the Excel formula and the OpenL formula moved as they were.
- `040-run` … `060-the-rule-still-reads-6` — the moved table calculates as before.

`050-a-new-version-keeps-the-formulas`

- `010-copy-as-a-new-version` — `CalcProduct` is copied as its version `0.0.2`.
- `020-the-copy-keeps-the-formulas` — the copy holds the Excel formula and the OpenL formula as the source does.
- `030-run` … `050-the-copy-reads-6` — the copy calculates as the source does.

`060-an-openl-formula-replaces-an-excel-one`

- `010-write-an-openl-formula` — the step `F4` of the copy is set to the OpenL formula `= $F1 * 5`.
- `020-the-cell-holds-the-text` — the Excel formula is gone, and the cell holds the text.
- `030-run` … `050-openl-calculates-240` — OpenL calculates the step: `2 * 3 * 4 * 10`.

`999-tierdown` closes the project without its changes and deletes it.

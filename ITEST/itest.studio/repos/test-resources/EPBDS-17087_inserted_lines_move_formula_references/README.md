# EPBDS-17087 — an inserted row or column moves the references of the Excel formulas

A row or a column inserted into a table moved its cells, but an Excel formula kept the addresses it named, so it
computed from the cells that came there instead: `=D4*3` read the value that moved into `D4`. Now the references follow
the cells they name, as in Excel, and a reference to a cell that stays keeps its address.

`EPBDS-17087.zip` is a project written for this. The Spreadsheet `Calc` stands at `B2:D5` with the steps `Base`, whose
`Value` is `10` and `Factor` is `2`, and `RETURN`, whose `Value` is `0`. The Datatype `Blocker` stands two rows under
it, so the Spreadsheet has room for one more row in place.

`010-setup` creates the project, opens it, lists its tables — every one compiling — and sets the value of `RETURN` to
the Excel formula `=D4*3`, the factor of `Base` times three. `Calc` returns `6`.

`020-a-column-inserted-before-the-value`

- `010-insert-a-column` — a column `Note` is inserted before `Value`, which moves `Value` and `Factor` right.
- `020-the-total-reads-the-moved-factor` — the formula reads `=E4*3`, where the factor moved, and computes `6`.
- `030-run` … `050-calc-still-reads-6` — `Calc` still returns `6`.

`030-a-row-inserted-above-the-base`

- `010-insert-a-row` — a step `Extra`, whose factor is `1`, is inserted above `Base`, which moves `Base` and `RETURN`
  down.
- `020-the-total-reads-the-moved-factor` — the formula reads `=E5*3`, not the factor of `Extra` at `E4`.
- `030-run` … `050-calc-still-reads-6` — `Calc` still returns `6`.

`040-a-row-the-table-has-no-room-for`

- `010-insert-a-row` — a step `More` is inserted above `Extra`; `Calc` has no room left above `Blocker` and moves.
- `020-the-table-moved` — `Calc` stands at `B11:E16`, under `Blocker`, and nothing fails to compile.
- `030-the-total-reads-the-factor-at-the-new-place` — the formula reads `=E15*3`, the factor of `Base` at its new
  place.
- `040-run` … `060-calc-still-reads-6` — `Calc` still returns `6`.

`050-a-column-deleted`

- `010-delete-the-note-column` — the column `Note` is deleted, which moves `Value` and `Factor` back left.
- `020-the-total-reads-the-factor-back` — the formula reads `=D15*3`.
- `030-run` … `050-calc-still-reads-6` — `Calc` still returns `6`.

`060-a-formula-rewritten-to-the-same-value` — EPBDS-17088: the version of a table changes with the text of its Excel
formulas, not only with the values they compute.

- `010-read-the-table` — the table is read, and the version it answers in `ETag` is kept.
- `020-rewrite-the-formula` — the formula of `RETURN` is rewritten as `=3*D15`, based on that version.
- `030-the-cell-holds-the-new-formula` — the cell holds the new formula and still computes `6`.
- `040-a-write-based-on-the-read-before-is-refused` — another edit based on the version of `010` is refused with
  `412`, as the table changed since that read.

`999-tierdown` closes the project without its changes and erases it.

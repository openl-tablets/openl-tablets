# EPBDS-16721 — the first column of a table is the reader's, like any other

A table's header is one cell banked across every column it has, and OpenL finds the table by the corner that
cell starts in. Laying a column down before the first one widens the bank over it; taking the first column
away narrows the bank. Neither moves the corner, which is why the Editor allowed both and why
`RawTableWriter` does.

The actions, though, still declared their position as `1..width`, left from a time when the first column was
read as the table's leading label. A request naming column 0 was refused by validation before the writer ever
saw it — `must be greater than or equal to 1` — whatever the table held.

The project is created from the bundled **Tutorial 1** template, so the table is the one a reader meets:
`DriverPremium2` on sheet `Step1`, led by the `# Rule` column that names its rules and is no condition of
its own.

`020-columns`

- `010-a-column-is-laid-down-before-the-first` / `020-the-header-widens-over-it` — the table grows to five
  columns and the header banks across all five.
- `030-the-first-column-goes` / `040-the-header-narrows-back-over-what-is-left` — the column laid down is
  taken away again and the table is back to the four it was read with, the header banked across them.

Both writes are a 400 against the unfixed code, naming `actions[0].target.position`.

`999-tierdown` closes the project and deletes it.

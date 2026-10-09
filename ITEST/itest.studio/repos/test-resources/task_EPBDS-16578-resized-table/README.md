# EPBDS-16578 — comparing a table that changed both its width and its height marks its cells

Two versions of a table are compared row by row when they keep their width and column by column when they keep
their height. A table that changed both is aligned first: its columns are paired by the values they share, its rows
are compared over the paired columns, and every cell of a row or a column only one version holds is marked.

`first.xlsx` holds `Datatype Person` of two columns and three rows. In `second.xlsx` the field `age` is renamed to
`years`, and the table gains a column of default values and the field `active`.

The suite holds no project, so it has no setup and nothing to tear down: `020-compare` starts the comparison of the
two files, waits for it, reads the table and drops the comparison.

- `030-the-table-is-changed-in-size` — the table is reported as changed, from `2x3` to `3x4`.
- `040-the-cells-that-differ-are-marked` — `C4` is marked on both sides; the second side also marks the added row
  `B5:C5` and the added column `D2:D5`.

# EPBDS-16599 — A table written as several partial tables

`PartialTables.xlsx` holds the same `SimpleRules` table twice over:

- `greeting` is split down the middle with `TablePart greeting row 1 of 2` and `row 2 of 2`, so the compiler
  gathers it from two places in the sheet and reads it through a composite grid;
- `farewell` is written in one piece, and is there to say what an ordinary table answers.

The suite covers what the editor needs to know about such a table:

- both ways of reading it — as the shape its kind gives it and as the grid the editor draws — mark it
  `partial`, and say nothing of the kind about the table written whole;
- a write to it is refused with `openl.error.400.table.partial.message` rather than failing on the grid it is
  read through — the cells it is drawn from do not sit together, so there is nowhere to write back into;
- the refused write leaves the table as it was.

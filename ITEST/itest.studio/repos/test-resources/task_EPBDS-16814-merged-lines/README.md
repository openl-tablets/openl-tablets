# EPBDS-16814 — taking lines away from a table with merged cells

An AI agent editing a lookup table through the raw table actions took one row away from a group merged in the
first column, and reported the table broken: gone from the table list, its module failing to compile. It also saw
a refused write leave the project marked as being edited. Neither happens on the current code, and this suite
keeps it so. It also covers what was found broken on the way: a block of lines reaching over two merged groups.

`EPBDS-16814.zip` is a project written for this. `Choices` is a smart rule whose first column holds groups of
codes merged into one cell each (`Fruit` and `Colour` of four rows, `Size` of three), with an `Integer` order
beside every code: a code shifted into the order column fails the compilation. `items` is a Data table with two
banks of two columns merged in one of its rows.

`010-setup` creates the project, opens it, and lists its tables — every one compiling.

`020-a-refused-write-leaves-the-project-opened`

- `010-a-row-left-empty-is-refused` — a row written blank would split the table, so it is refused.
- `020-the-project-is-still-opened` — the refused write took nothing: the project is not marked as being edited.

`030-one-row-inside-a-merged-group`

- `010-take-away-the-last-row-of-a-group` — `Colour / Black`, the last row of its group.
- `020-the-group-is-one-row-shorter` — only that row is gone, and `Colour` spans three rows.
- `030-every-table-compiles` — the table is still listed, one row shorter, and nothing fails to compile.

`040-rows-across-two-groups`

- `010-take-away-the-end-of-one-group-and-the-start-of-the-next` — `Fruit / Date` and `Colour / Red`, the row
  the value of `Colour` stood on.
- `020-each-group-loses-its-own-row` — `Fruit` spans three rows, and `Colour` keeps its value over the two left.
  Against the unfixed code the write fails: `Fruit` was shortened by the whole block and `Colour` was not fitted
  at all, leaving the two merges overlapping.
- `030-every-table-compiles`

`050-columns-across-two-banks`

- `010-take-away-the-end-of-one-bank-and-the-start-of-the-next` — columns `b` and `c`.
- `020-each-bank-keeps-its-value` — both banks are left one cell wide, with `x` and `y` still in them.
- `030-every-table-compiles`

`999-tierdown` closes the project, dropping the changes, and deletes it.

# EPBDS-16990 — rows taken away from a table leave the rows below as they were stored

Taking rows away from a table moved every cell below them by reading its value and writing it back, so the cells
were saved in another notation: `24` as `24.0`, `0.0003` as `3.0E-4`, a date as a number in a new style. The values
stayed the same, but the commit showed hundreds of changed cells. Now a moved cell keeps what it stores.

`EPBDS-16990.zip` is a project written for this. `OptionsOf` is a smart rule that collects `Option` rows, grouped by
a first column merged over three rows each. Its cells are stored as Excel stores them: whole and decimal numbers,
booleans — some as a `TRUE()` formula with its result — dates in a format of their own, and texts.

`010-setup` creates the project, opens it, and lists its tables — every one compiling.

`020-take-away-two-rows`

- `010-take-away-the-first-two-rows` — `Fruit / Apple` and `Fruit / Pear`.
- `020-the-rows-below-are-saved-as-they-were` — the workbook stores the moved cells as before: `26`, `0.0003`, the
  formula with its result and the date number.
- `030-every-table-compiles` — the table is two rows shorter, and nothing fails to compile.

`030-compare`

- `010-history` — the module has the version it was opened at and the edited one.
- `020-start-comparison` … `030-wait-comparison` — the two versions are compared.
- `040-only-the-table-changed` — the datatype is equal, the smart rule is two rows shorter.
- `050-only-the-rows-taken-away-differ` — only the cells of the two rows taken away are marked.
- `060-drop` — the comparison is released.

`040-a-comment-longer-than-a-repository-keeps` — EPBDS-16994: a repository keeps a comment of at most 255
characters, and a longer one was refused with no word of what was too long.

- `010-the-save-is-refused` — a save with a comment of 256 characters is refused, naming the comment.
- `020-the-delete-is-refused` — so is a delete.
- `030-the-project-keeps-its-changes` — nothing was saved: the project is still being edited.

`999-tierdown` closes the project without its changes and deletes it.

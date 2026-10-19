# EPBDS-16926 — an id a table had before keeps naming it, and the id of a deleted table answers 410

A table id is made of the module, the kind and the name of the table, so renaming a table, or moving its workbook,
gives it a new id. A client holding the id from before is served the table under its current id, with the current id
in the body and the address under it in `Content-Location`. An id of a table OpenL Studio deleted answers `410`.

`010-setup` creates a project, opened, whose module `rules/Rates.xlsx` holds the datatypes `Greeting` and `Farewell`.

`020-listing` reads both datatypes by their ids, which numbers the tables of the module.

`030-rename-in-studio`:

- `010-the-editor-renames-a-datatype` — `Greeting` becomes `Salute`, and the write answers its new id.
- `020-the-old-id-reads-the-renamed-datatype` — the id of `Greeting` reads `Salute`, naming it in `Content-Location`.
- `030-a-write-by-the-old-id-answers-the-current-one` — a write by the old id writes `Salute` and answers its id.

`040-rename-in-excel` — a workbook in which `Farewell` is named `Adieu` at the same cell is uploaded over the module;
the compilation that follows finds the rename, and the id of `Farewell` reads `Adieu`.

`050-move-the-workbook` — the workbook moves to `rules/Prices.xlsx`, which gives its tables new ids; the id `Salute`
had in `rules/Rates.xlsx`, and the id of `Greeting` before it, read `Salute` under its new id.

`060-delete` — `Salute` is deleted: its id, and every id it had before, answer `410`.

`999-tierdown` closes the project and deletes it.

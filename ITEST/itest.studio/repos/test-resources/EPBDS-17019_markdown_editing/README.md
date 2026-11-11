# EPBDS-17019 — the tables of a module of Markdown files are edited through the REST API of OpenL Studio

`markdown-editing.zip` holds a project as the Empty Project template makes it: a `rules.xml` with no modules and the
workbook `rules/Main.xlsx`. Every module of Markdown files is created by the requests, and the project is not saved
in between, so the files are read back from the working copy they are written to.

`010-setup` creates the project and opens it.

`020-new-modules` — a table of a new module creates its folder:

- `010` — a Datatype creates the module `Model`, `020` — its file is `rules/Model/Driver.md`.
- `030` — an Environment creates the module `Rating`, `040` — its file is `Environment.md` at the root of the folder,
  whatever the sheet.
- `050` — the sheets of `Rating` are read from its folder while the project is not saved yet.
- `060` — a second Environment of the module is refused.
- `070` — the module `Rating.v2` is the folder `rules/Rating-v2`, and the sheet named after the module is its root.

`030-edits` — each edit of `ageFactor` changes only the lines of the file it touches:

- `020`–`030` — a value; `040` — a row inserted.
- `050` — cells of two values are not merged; `060`–`070` — two cells of one value are, the range written under
  `merges`.
- `080`–`100` — the cells unmerged and the row deleted take the range and the row out of the file.
- `110` — a style other than the indent is refused, `120` — an Excel formula too.
- `130`–`160` — a property goes into the front matter and the description above the table; the last property removed
  takes `properties` away.

`040-copies`:

- `010`–`020` — a copy into another folder of the module is a file of its own.
- `030` — a copy that keeps the name needs a version of its own.
- `040`–`060` — a copy as a new version is a file of its own, and the source stands down in its file. The properties
  the copy names stand for those of the source, so the copy has no description.

`050-records` — a record of a Data table is one row, `040` — and the table merges no cells, even of one value.

`060-removal` — a table is removed with its file, `020` — which is not found any more; `030` — the last table of a
module stays.

`999-tierdown` closes the project and deletes it.

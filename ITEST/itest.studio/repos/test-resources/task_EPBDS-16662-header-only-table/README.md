# EPBDS-16662 — a table begun as a header alone does not take the module's listing down

An author writing a table leaves a header with nothing under it for a while — `Spreadsheet ` in a cell, and no
rows yet. The engine answers no body for such a table. Asking for its body's height without looking failed the
listing of every table in the module with HTTP 500, so the module never opened in the editor; and the engine binds
no body view for such a table, so a form written over it took the header row for the body and wrote over it.

`010-setup` creates a project, opened, whose one module holds a finished datatype, a spreadsheet that is a header
alone, and a datatype that is a header alone.

`020-listing`:

- `010-the-module-lists-its-tables-the-unfinished-ones-among-them` — the listing answers all three tables; the
  unfinished spreadsheet is listed by its kind, with the error the compiler raised on it.
- `020-the-unfinished-table-reads-as-the-grid-it-is` — read without asking for the grid, the spreadsheet is
  answered as the grid it is: the readers shaped by a body decline it rather than failing on the rows it does not
  have.
- `030-the-editor-reads-it-raw` — the read the editor makes answers the same grid.

`030-completion` — the unfinished tables are completed the two ways a table is written:

- `010-the-editor-completes-the-spreadsheet-with-the-grid` — the grid the editor saves, header and body rows,
  goes over the header alone.
- `020-the-completed-spreadsheet-compiles` — the listing now names it a `SimpleSpreadsheet` with no error.
- `030-a-form-completes-the-datatype-under-its-header` — a datatype form written over the header alone.
- `040-the-datatype-reads-back-with-its-fields` — the header stayed where it was and the fields stand under it,
  rather than the header row being written over as if it were the body.

`999-tierdown` closes the project and deletes it.

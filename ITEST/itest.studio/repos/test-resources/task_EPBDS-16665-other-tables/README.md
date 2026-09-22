# EPBDS-16665 — the utility tables are listed when a reader asks for them

A table OpenL does not recognize — a note an author left beside the rules, a block whose first cell starts with no
table keyword — is a free-form table, filed under the kind `Other`. The Editor's tree hid them as utility tables
unless its filter said otherwise; the tables list left them out and nothing could ask for them, so a module holding
one never showed it anywhere.

`010-setup` creates a project, opened, whose one module holds a rules table and two free-form tables, both named
`Test123` by their first cell, as the workbook of the ticket names them.

`020-listing`:

- `010-the-tree-lists-the-rules-and-leaves-the-utility-tables-out` — asked as the tree asks, the module lists the
  rules table alone.
- `020-asked-for-the-utility-tables-are-listed-among-the-rest` — asked with `includeOther=true`, the two free-form
  tables are listed with it, each by what its first cell says, with no signature made of the rest of that cell.
- `030-a-search-narrowed-to-their-kind-finds-them-without-the-flag` — a `kind=Other` filter asks for nothing else,
  so the extended search finds them without the flag.

`030-editing` — a free-form table is edited as the grid it is, and in no other way:

- `010-a-utility-table-reads-as-the-grid-it-is` — the read the editor makes answers its cells.
- `020-no-properties-can-be-written-on-it` — a table of this kind carries no properties, so the properties API
  refuses it.
- `030-the-editor-writes-its-edits-as-actions` — the edits the editor saves, a cell changed and a row appended,
  go over it the way they go over any table.
- `040-the-edits-read-back` — the cell and the row are there.
- `050-the-whole-grid-goes-over-it-when-the-request-says-what-kind-it-is` — the grid written whole is taken when
  the request declares the `Other` kind, which is what waives the check for a recognized header.
- `060-the-grid-reads-back` — the grid is what was written.
- `070-the-tree-lists-the-edited-table-where-it-grew` — the listing names its new extent.

`999-tierdown` closes the project and deletes it.

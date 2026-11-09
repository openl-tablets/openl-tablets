# EPBDS-17221 — a table under a note keeps its header unmerged

`TableLost.zip` holds the workbook attached to the ticket, as it is. Its sheet `Data` has the Data table `persons`
at `A2:C6` and the note `note above the third column` in `C1`, right above the empty third cell of the header. The
sheet `Types` has the Datatype `Person` of the rows.

Saving a table formats it, which merges its header across the table. Merged, the header touched the note: OpenL read
the note and the table as one block starting at the empty `A1`, and the table `persons` was lost from its module.
Now a header is left unmerged under a block of filled cells that ends right above it and starts over one of its cells
after the first.

`010-setup` creates the project, opens it, and lists its tables — every one compiling.

`020-edit-a-value`

- `010-change-ann-to-anna` — the value `Ann` of the table `persons` is changed to `Anna`, which saves the table.
- `020-every-table-still-compiles` — the table `persons` stays at `A2:C6`, beside the Datatype `Person`.
- `030-the-table-holds-the-value` — the table holds `Anna`, its header unmerged.

`999-tierdown` closes the project without its changes and deletes it.

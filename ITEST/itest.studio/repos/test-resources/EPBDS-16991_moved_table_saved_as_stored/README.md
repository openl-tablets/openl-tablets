# EPBDS-16991 — a table moved to grow keeps what its cells store

A table that has no room to grow in place is moved where there is room. The move wrote every cell again by its
value: a text lost its leading and trailing spaces, `1` was stored as `1.0`, and the merged title was written into
every cell it covers. Now the cells of the table move as they are stored.

`EPBDS-16991.zip` is a project written for this. The Data table `first` has another table, `second`, one row below
it. Its field row holds ` id` with a leading space and its display row `Name ` with a trailing one, its ids are
stored as Excel stores them, and its title is merged over both columns.

`010-setup` creates the project, opens it, and lists its tables — every one compiling.

`020-append-three-rows`

- `010-append-rows-the-table-has-no-room-for` — three rows appended to `first`, which moves below `second`.
- `020-the-moved-cells-are-saved-as-they-were` — the moved table keeps ` id`, `Name ` and the ids `1`, `2`, `3` as
  they were stored, and its title stays in the first cell of the merge only.
- `030-every-table-compiles` — `first` stands at `B20:C28`, `second` where it was, and nothing fails to compile.

`030-append-a-whole-number` — EPBDS-16992: a whole number written into a cell was saved as Java writes a `double`,
`67.0`, beside the cells Excel saved as `66`.

- `010-append-a-row-with-a-json-integer` — a row with the id `67`, sent as a JSON number.
- `020-whole-numbers-are-saved-as-excel-saves-them` — the new id is stored as `67`, the id `4` appended before as
  `4`, beside the moved id `1`.
- `030-every-table-compiles` — `first` is one row longer, and nothing fails to compile.

`999-tierdown` closes the project without its changes and deletes it.

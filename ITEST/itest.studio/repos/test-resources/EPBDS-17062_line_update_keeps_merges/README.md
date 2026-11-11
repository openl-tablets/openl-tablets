# EPBDS-17062 — an update of a row or a column keeps its merged cells

`Rates.zip` holds one workbook built for the ticket: the SmartLookup `Rate` at `B2:E8` of the sheet `Rates`. The
duration `<=4` is merged over the rows of `Male` and `Female`, and so is `>4`; the fallback row holds one OpenL formula,
`= error("No rate")`, merged over both periods.

An update of a row or a column used to drop every merged cell starting in it unless the request named its span again.
A client writing values back — the MCP server sends only values — lost the groups: the rows they covered read as
empty conditions, matching any input, and the fallback answered for one period only.

`010-setup` creates the project, opens it, lists the table — compiling — and reads it as written.

`020-a-row-written-back-keeps-its-group`

- `010-update-row` — row 2 is sent back as values only, with one rate changed.
- `020-the-group-stays-merged` — `<=4` still spans rows 2 and 3.
- `030-run` to `050-result` — a duration of 10 for `Female` and `4 - 4` gets the rate `0.7` of the group `>4`. With the
  group of row 2 dropped, row 3 matched any duration and answered `0.3`.

`030-a-column-written-back-keeps-the-fallback`

- `010-update-column` — column 2 is sent back as values only, its rates changed, the fallback without its span.
- `020-the-fallback-stays-merged` — the fallback still spans columns 2 and 3.
- `030-run` to `050-result` — a gender no row holds gets the error `No rate` for `8 - 8`, the column not written. With
  the fallback reduced to column 2, the rule answered no result.

`040-a-value-under-a-merged-cell-is-refused`

- `010-update-the-fallback-row` — a value for column 3 of the fallback row, which the kept merged cell covers, is
  refused with `400`; `unmerge` splits the cell first.

`999-tierdown` closes the project without its changes and deletes it.

# A change of yours lost when both branches shift a table that has another table beside it

The workbooks are written as Excel writes them. In `base.xlsx` the table `discount` at A1 has the property
`category` set to `Auto`, and the table `rate` stands beside it at E1, on the same rows. Each branch shifts the cells
of `discount` alone down and adds the property `lob` above `category`: master sets it to `Auto` (`yours.xlsx`), the
side branch to `Personal` (`theirs.xlsx`). Receiving the side branch into master answers `conflicts`, the table
`discount` with the reason `rows`, and cancelling the merge leaves master with `lob` set to `Auto`.

Master sets `lob` to the value `category` held at the same cell before the shift. Sync used to miss the shift of a
part of the rows, merge the sheet cell by cell, take the value of the side branch there and lose the one of master
with no conflict.

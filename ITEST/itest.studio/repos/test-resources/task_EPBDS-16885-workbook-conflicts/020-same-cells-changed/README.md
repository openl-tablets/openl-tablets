# The same cells changed in both branches

Both branches change the rate of US in `rate`: 1.5 on the side branch, 1.4 on master. The side branch changes
the rate of DE as well, which master leaves. The table is a `cells` conflict, and each version marks the cells
its branch changed. The table comes merged cell by cell too: the rate of DE is merged already, and the rate of US
is the one conflicting cell, at row 2, column 1 of the merged table, with 1.4 in your version and 1.5 in theirs.

A merge that leaves the conflict undecided, or decides it twice, is refused. Taking their version answers the
merged workbook, and so does merging the table cell by cell, with their rate of US or with a value typed for it.
A merge cell by cell that leaves the conflicting cell undecided, names a cell outside the merged table, or decides
the table both whole and cell by cell, is refused. The merge stays a conflict until the merged workbook is saved:
cancelling it leaves master with the rates of master.

This is the expected behaviour, not a defect.

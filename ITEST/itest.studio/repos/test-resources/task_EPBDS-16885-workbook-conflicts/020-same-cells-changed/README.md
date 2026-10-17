# The same cells changed in both branches

Both branches change the rate of US in `rate`: 1.5 on the side branch, 1.4 on master. The side branch changes
the rate of DE as well, which master leaves. The table is a `cells` conflict, and each version marks the cells
its branch changed.

A merge that leaves the conflict undecided, or decides it twice, is refused. Taking their version answers the
merged workbook, and the merge stays a conflict until the merged workbook is saved: cancelling it leaves master
with the rates of master.

This is the expected behaviour, not a defect.

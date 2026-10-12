# Row inserted and another row changed in one table

The side branch inserts FR between US and DE in `rate`, master changes the rate of CA in the same table.
Sync aligns the rows of `rate` with the base: the change of master follows CA to its new row, and the merge
succeeds with both changes.

# Row inserted inside one table, another table changed

The side branch inserts FR between US and DE in `rate`, master changes the minimum age of NY. The inserted
row moves the rows below it inside `rate`; Sync aligns the rows of the table with the base, so the merge
succeeds with both changes.

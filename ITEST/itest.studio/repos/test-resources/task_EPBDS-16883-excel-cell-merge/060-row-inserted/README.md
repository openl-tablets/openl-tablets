# Row inserted inside one table, another table changed

The side branch inserts FR between US and DE in `rate`, master changes the minimum age of NY. The inserted
row moves the rows below it, so a cell of master no longer stands where the same cell of the side branch
does. Sync does not merge moved content cell by cell yet: the merge answers `conflicts`, and cancelling it
leaves master as it was.

This is the expected behaviour of the first step of EPBDS-16795, not a defect.

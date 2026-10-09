# Different tables of one sheet changed

The side branch changes the minimum age of CA in `minAge`, master changes the rate of US in `rate`. Both
tables are on one sheet, so the sheet changed in both branches. The changes are in different cells, so Sync
merges the sheet cell by cell: the merge answers `success`, both tables hold both changes, and the merge
commit lists the sheet with both branches.

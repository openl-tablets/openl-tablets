# Theme applied to a table, its values changed

The side branch saves `rate` while the tables are formatted on save (`formatTablesOnSave` of the system settings), so
the save restyles every cell of the table with the table theme; master changes the rates of US and CA in the same
table. The value and the style of a cell merge separately, so the merge answers `success`: `rate` holds the new rates
in the colors of the theme.

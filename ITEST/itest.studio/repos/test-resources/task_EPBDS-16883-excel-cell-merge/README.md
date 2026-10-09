# Merging a workbook sheet changed in both branches

Sync merges a sheet of a workbook that both branches changed cell by cell, against the common base: a cell
changed in one branch takes that branch's content, and a cell changed differently in both branches is a
conflict. While a branch inserts or deletes rows or columns inside the content, the sheet stays a conflict.

Every scenario writes the same workbook `Rates.xlsx` on master, branches from it, edits the tables `rate` and
`minAge` of its sheet `Rules` through the table actions on the side branch and on master, and receives the
side branch into master.

| Scenario | Changes | Result |
|----------|---------|--------|
| `020-different-tables` | Each branch changes its own table | merged automatically |
| `030-different-rows` | Each branch changes its own row of one table | merged automatically |
| `040-row-appended` | One branch appends a row, the other changes another table | merged automatically |
| `050-same-cell` | Both branches change one cell differently | conflict, manual resolution |
| `060-row-inserted` | One branch inserts a row inside a table | conflict, manual resolution |
| `070-other-file-resolved-manually` | Another file conflicts as well | the file by hand, the workbook automatically |
| `080-theme-and-values` | One branch applies a table theme, the other changes values of that table | merged automatically |

A conflict here is the expected result, not a defect.

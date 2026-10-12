# Merging tables of a workbook sheet changed in both branches

Sync merges a sheet of a workbook that both branches changed table by table, against the common base. A table
changed in both branches is merged row by row: rows inserted, deleted or changed in one branch are taken from
it, and a table one branch moved keeps the changes the other branch made in it. A cell both branches changed
differently stays a conflict.

Every scenario writes the same workbook `Rates.xlsx` on master, branches from it, edits the tables `rate` and
`minAge` of its sheet `Rules` through the table actions on the side branch and on master, and receives the
side branch into master.

| Scenario | Changes | Result |
|----------|---------|--------|
| `020-row-inserted-and-row-changed` | One branch inserts a row, the other changes another row of the table | merged automatically |
| `030-table-moved` | One branch grows a table until OpenL Studio moves it, the other changes it | merged automatically |
| `040-rows-inserted-at-the-same-place` | Both branches insert a row at the same place | conflict, manual resolution |
| `050-row-deleted-and-changed` | One branch deletes a row the other changes | conflict, manual resolution |

A conflict here is the expected result, not a defect.

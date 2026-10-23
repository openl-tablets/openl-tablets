# Resolving the conflicts of a workbook table by table

Sync leaves a workbook a conflict when both branches changed a table in ways the merge cannot combine. The
Resolve Conflicts dialog then reads the conflicting tables of the workbook with
`GET /merge/conflicts/workbook`, and `POST /merge/conflicts/workbook` merges the workbook again with each
conflicting table taken whole from the version decided: yours, theirs or the base one. A table only some cells of
which conflict comes merged cell by cell as well (EPBDS-16964), and is merged again with the version decided for
each conflicting cell or for the table, and the values typed. Neither request changes the conflict: the client saves
the merged workbook as an uploaded file.

Every scenario writes the same workbook `Rates.xlsx` on master, branches from it, edits the table `rate` of its
sheet `Rules` through the table actions on the side branch and on master, receives the side branch into master,
and cancels the merge at the end. What the merged workbook holds is checked by the tests of the merge itself:
here a merge answers the workbook, and changes nothing.

| Scenario | Changes | Result |
|----------|---------|--------|
| `020-same-cells-changed` | Both branches change the rate of US, their branch the rate of DE as well | the table is a `cells` conflict, merged cell by cell with the rate of US left to decide; taking their version, or merging the table cell by cell, answers the merged workbook |
| `030-row-inserted-next-to-a-deleted-one` | Our branch inserts a row before the one their branch deletes | the table is a `rows` conflict, its inserted row marked, which is not merged cell by cell; discarding both changes answers the merged workbook |

A conflict here is the expected result, not a defect.

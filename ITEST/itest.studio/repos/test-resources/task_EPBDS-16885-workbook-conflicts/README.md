# Resolving the conflicts of a workbook table by table

Sync leaves a workbook a conflict when both branches changed a table in ways the merge cannot combine. The
Resolve Conflicts dialog then reads the conflicting tables of the workbook with
`GET /merge/conflicts/workbook`, and `POST /merge/conflicts/workbook` merges the workbook again with each
conflicting table taken whole from the version decided: yours, theirs or the base one. Neither request changes
the conflict: the client saves the merged workbook as an uploaded file.

Every scenario writes the same workbook `Rates.xlsx` on master, branches from it, edits the table `rate` of its
sheet `Rules` through the table actions on the side branch and on master, receives the side branch into master,
and cancels the merge at the end. What the merged workbook holds is checked by the tests of the merge itself:
here a merge answers the workbook, and changes nothing.

| Scenario | Changes | Result |
|----------|---------|--------|
| `020-same-cells-changed` | Both branches change the rate of US, their branch the rate of DE as well | the table is a `cells` conflict; taking their version answers the merged workbook |
| `030-rows-inserted-at-the-same-place` | Both branches insert a row at the same place | the table is a `rows` conflict, its inserted rows marked; discarding both changes answers the merged workbook |

A conflict here is the expected result, not a defect.

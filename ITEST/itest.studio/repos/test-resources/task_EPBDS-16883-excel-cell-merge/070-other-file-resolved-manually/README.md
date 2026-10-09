# Another file resolved by hand while the workbook is merged

Both branches change the same line of `notes.txt`, and each changes its own table of the workbook. The notes are
a conflict, the workbook is not: the merge answers `conflicts` for `notes.txt` only. Resolving the notes by hand
merges the workbook cell by cell too, and the merge commit lists the workbook sheet with both branches next to
the resolved notes.

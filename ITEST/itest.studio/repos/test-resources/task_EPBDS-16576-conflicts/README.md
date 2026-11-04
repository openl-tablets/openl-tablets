# Comparing the two versions of a conflicted file

This suite verifies that the file a merge could not settle is compared through the Compare API:

- a project is branched, and the same table is added to `Main.xlsx` on each branch, so receiving
  the side branch leaves the workbook in conflict;
- the conflicted workbook is compared as the merge holds it, the version being received against the
  version of the workspace;
- the base version is compared with the version being received, which tells what the side branch
  changed: the sheet and the table it added;
- the base version is compared with both versions at once, which tells for each table what each branch did
  to it, and a table is read in the three versions, lined up by the base;
- the comparison answers with its own identifier and is read, watched and released as any other one;
- once the merge is cancelled, the working copy is compared with the last revision of the side branch: the changed
  files come with the common revision both branches parted at, and the workbook is named a conflict, as a merge of
  both stops at it;
- a conflicted file that is not an Excel file is refused before a comparison is started, because the
  screen reads such a file line by line instead.

The comparison reads copies of the two versions, so cancelling the merge afterwards leaves it readable.

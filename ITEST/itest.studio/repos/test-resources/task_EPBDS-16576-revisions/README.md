# Comparing the working copy of a project with a revision

This suite verifies that two Excel files of one project are compared through the Compare API:

- the Excel files that can be compared are listed for the working copy and for a revision alike;
- a file of the working copy is compared with the same file as a revision holds it;
- the comparison answers with its own identifier and is read, watched and released as any other one;
- a file that is not an Excel file is refused before a comparison is started.

The repository this runs against keeps its revisions per path rather than per commit, so the suite also
covers reading a revision from a repository that has no branches.

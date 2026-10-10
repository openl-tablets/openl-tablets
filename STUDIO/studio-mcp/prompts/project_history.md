---
title: Project History
description: Guide for viewing a project's committed Git history with openl_repository_project_revisions and its local workspace changes with openl_list_project_local_changes, and when to use each
arguments:
  - name: projectId
    description: ID of the project
    required: false
---

## Summary

OpenL tracks two kinds of history:

- **Committed history** — the Git commit log of a project in a design repository. Read it with **openl_repository_project_revisions**.
- **Local workspace history** — uncommitted versions of a specific module while its project is open. List them with **openl_list_project_local_changes** and roll back with **openl_restore_project_local_change**.

History applies only to projects in a design repository. For `repository: 'local'`, neither committed revisions nor local change history is available (local projects cannot be opened).

# Project History: **{projectId}**

{if projectId}
Use the tools below to inspect history for **{projectId}**.
{end if}

## Committed history — openl_repository_project_revisions

Use this for the project's Git commit log. It is read-only and does not require the project to be opened.

- Audit the change trail across the entire OpenL project
- See who committed what and when, across multiple Excel files
- Track team activity and review past saves/merges

Key arguments:
- `projectId` — the stable project ID returned by **openl_list_projects**; use it unchanged rather than deriving it from the displayed name
- `search` — optional filter on commit message or author
- `techRevs` — optional, include technical revisions (default: false)
- `offset` or `page` / `size` — optional pagination; `offset` and `page` are mutually exclusive (size default 50, max 200)

The history is read from the branch the project is currently on. Addressing it by ID keeps the history available when `rules.xml` contains an unsaved project rename and the repository still uses the published name.

## Local workspace history — openl_list_project_local_changes

Use this for the uncommitted change history of a module in a project you have open. Call **openl_open_project** first, then **openl_list_project_modules** to obtain the exact module name. The history endpoint addresses the project and module explicitly instead of relying on whichever module happens to be current in the HTTP session.

- Review versions saved locally before they are committed
- Find an earlier local version to recover from

Both local-history tools require:

- `projectId` — the stable project ID returned by **openl_list_projects**
- `moduleName` — the exact module name returned by **openl_list_project_modules**

To roll back, pass those same identifiers and the `historyId` from the list response to **openl_restore_project_local_change**. This overwrites that module's current local state, so confirm before restoring.

## Choosing between them

- Need the committed Git log, authors, and commit hashes → **openl_repository_project_revisions**
- Need to inspect or undo uncommitted local edits in an open project → **openl_list_project_local_changes** / **openl_restore_project_local_change**

## OpenL Revision Info

Each revision from **openl_repository_project_revisions** includes:
- commitHash (Git SHA)
- Author, timestamp
- Branch, commit type (SAVE, MERGE, etc.)
- Files changed, tables changed

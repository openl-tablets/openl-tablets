---
title: "Local Projects (repository: local)"
description: "Working with projects in repository 'local': no open/save/close, no Git; table/rule/test tools work directly without opening"
---

## Summary

For projects with **repository: 'local'** (local-only, stored as files on disk, no branches or commits):

- **Never check or require OPENED/EDITING** — local projects are always considered editable.
- **Avoid calling** `openl_open_project`, `openl_save_project`, or `openl_close_project`; do not use Git tools (branches, history, revert).
- **Use** `openl_list_projects` (call without repository filter, follow pagination to completion, then filter results by `repository === "local"`—filter parameter "local" often fails), `openl_get_project`, and table/test tools (`openl_list_tables`, `openl_get_table`, `openl_update_table`, `openl_append_table`, `openl_create_project_table`, `openl_start_project_tests`, `openl_get_test_results_summary`, `openl_get_test_results`, `openl_get_test_results_by_table`) directly — no open step. Tests for local projects run without opening the project.
- **Use RawSource only** for table content: get returns the raw matrix and create/update/append accept only `tableType: "RawSource"`. Preserve Studio's context-parsed one-dimensional multi-value arrays; never construct typed Rules/Spreadsheet/Datatype/Test payloads or guess OpenL array text.

# Local projects (repository: local)

## What is "local"

- Projects in repository **'local'** are stored only on the server disk, without Git.
- No branches, no commits, no open/save/close workflow.

## Rules for agents

1. **Never require OPENED/EDITING** for `repository === 'local'`.
2. **Never call** for local:
   - `openl_open_project`
   - `openl_save_project`
   - `openl_close_project`
   - `openl_list_branches`, `openl_create_project_branch`
   - `openl_repository_project_revisions` (committed revisions require Git; local has none)
   - `openl_list_project_local_changes`, `openl_restore_project_local_change` (module local history requires an opened project; local cannot be opened)
3. **Use directly** for local (no open first):
   - `openl_list_projects` (list all pages of projects, then filter by `repository === "local"` in the response—the repository filter may fail because "local" is often not returned by openl_list_repositories), `openl_get_project`
   - `openl_list_tables` (follow pagination to completion when a complete table inventory is required), `openl_get_table`, `openl_update_table`, `openl_append_table`, `openl_create_project_table`
   - `openl_start_project_tests`, `openl_get_test_results_summary`, `openl_get_test_results`, `openl_get_test_results_by_table`

## Short rule

For **repository: 'local'** — never call open/save/close; never require EDITING; table and test operations are allowed directly.

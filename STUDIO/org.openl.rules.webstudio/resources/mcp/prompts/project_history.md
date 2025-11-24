## Summary

**Project-wide audit trail**: Use openl_get_project_history() for all commits across entire project (multiple files), openl_get_file_history() for single file tracking. Each commit shows author, files changed, tables modified, and commit type (SAVE/MERGE/etc).

# OpenL Project History vs File History

{if projectId}
## Project History: **{projectId}**
{end if}

## When to Use

**USE openl_get_project_history WHEN:**
- Need audit trail across entire OpenL project
- Find when change affected multiple Excel files
- Track team activity across project
- Compare project states at different Git commits
- Revert entire project to stable commit

**USE openl_get_file_history WHEN:**
- Track single Excel file changes
- Find who modified specific file
- Compare file versions
- Recover deleted file

## Operations

**Project audit:**
```text
openl_get_project_history({if projectId}projectId="{projectId}", {end if}limit=50) → All commits, all files
```

**Find specific change:**
```text
openl_get_project_history({if projectId}projectId="{projectId}", {end if}limit=200) → search commit comments
```

**Compare project states:**
```text
1. openl_get_project_history({if projectId}projectId="{projectId}"{end if}) → get commitHashes
2. Note: openl_compare_versions is temporarily disabled - use OpenL WebStudio UI for comparison
```

**Revert project:**
```text
1. openl_get_project_history({if projectId}projectId="{projectId}"{end if}) → find stable commitHash
2. openl_revert_version(targetVersion=commitHash)
```

## OpenL Commit Info

Each commit shows:
- commitHash (Git SHA)
- Author, timestamp
- Branch, commit type (SAVE, MERGE, etc.)
- Files changed, tables changed

## Pagination
- `limit`: 50 (default, max 200), `offset`: 0

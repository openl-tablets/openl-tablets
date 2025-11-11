# OpenL Project History vs File History

## When to Use

**USE project_history WHEN:**
- Need audit trail across entire OpenL project
- Find when change affected multiple Excel files
- Track team activity across project
- Compare project states at different Git commits
- Revert entire project to stable commit

**USE file_history WHEN:**
- Track single Excel file changes
- Find who modified specific file
- Compare file versions
- Recover deleted file

## Operations

**Project audit:**
```
get_project_history(limit=50) → All commits, all files
```

**Find specific change:**
```
get_project_history(limit=200) → search commit comments
```

**Compare project states:**
```
1. get_project_history() → get commitHashes
2. compare_versions(baseCommitHash, targetCommitHash)
```

**Revert project:**
```
1. get_project_history() → find stable commitHash
2. revert_version(targetVersion=commitHash)
```

## OpenL Commit Info

Each commit shows:
- commitHash (Git SHA)
- Author, timestamp
- Branch, commit type (SAVE, MERGE, etc.)
- Files changed, tables changed

## Pagination
- `limit`: 50 (default, max 200), `offset`: 0

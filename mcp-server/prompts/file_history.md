---
name: file_history
description: Git-based version history for OpenL files with commit operations
arguments:
  - name: filePath
    description: Path of the file to view history for
    required: false
  - name: projectId
    description: ID of the project containing the file
    required: false
---

# OpenL File History (Git-Based Versioning)

{if filePath}
## File History: `{filePath}`
{end if}
{if projectId}

**Project**: {projectId}
{end if}

OpenL uses Git for version control. Every save/upload creates Git commit automatically.

## Version = Git Commit Hash
Versions are commit hashes (e.g., "7a3f2b1c"), NOT v1/v2/v3

## Common Operations

**VIEW history:**
```
get_file_history({if filePath}filePath="{filePath}", {end if}limit=20)
```

**FIND specific date:**
```
get_file_history({if filePath}filePath="{filePath}", {end if}limit=100) → filter by date → find commitHash
```

**DOWNLOAD old version:**
```
download_file(fileName, version=commitHash)
```

**COMPARE versions:**
```
compare_versions(baseCommitHash, targetCommitHash)
```

**RECOVER deleted file:**
```
1. get_file_history({if filePath}filePath="{filePath}"{end if}) → find last SAVE commit before ERASE
2. download_file(fileName, version=lastGoodCommitHash)
```

**REVERT:**
```
revert_version(targetVersion=oldCommitHash) → Creates NEW commit
```

## OpenL Commit Types

- **SAVE**: Normal save operation
- **ARCHIVE**: File archived (soft delete)
- **RESTORE**: File restored from archive
- **ERASE**: Permanently deleted
- **MERGE**: Git branch merge

## Pagination
- `limit`: 50 (default), `offset`: 0
- Large history → use offset: 50, 100, 150...

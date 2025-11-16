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

## Summary

**Track file changes with Git commit history**: Every save creates a Git commit with hash (not v1/v2). Use openl_get_file_history() to view commits, openl_download_file(version=hash) for old versions, openl_revert_version() to restore previous state.

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
openl_get_file_history({if filePath}filePath="{filePath}", {end if}limit=20)
```

**FIND specific date:**
```
openl_get_file_history({if filePath}filePath="{filePath}", {end if}limit=100) → filter by date → find commitHash
```

**DOWNLOAD old version:**
```
openl_download_file(fileName, version=commitHash)
```

**COMPARE versions:**
```
Note: openl_compare_versions is temporarily disabled - use OpenL WebStudio UI for version comparison
```

**RECOVER deleted file:**
```
1. openl_get_file_history({if filePath}filePath="{filePath}"{end if}) → find last SAVE commit before ERASE
2. openl_download_file(fileName, version=lastGoodCommitHash)
```

**REVERT:**
```
openl_revert_version(targetVersion=oldCommitHash) → Creates NEW commit
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
